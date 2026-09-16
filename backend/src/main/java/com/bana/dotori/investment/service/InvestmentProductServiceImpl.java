package com.bana.dotori.investment.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentHoldingsResponse;
import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.entity.InvestmentPriceHistory;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.entity.InvestmentTransaction;
import com.bana.dotori.investment.entity.InvestmentTransactionType;
import com.bana.dotori.investment.mapper.InvestmentProductMapper;
import com.bana.dotori.investment.repository.InvestmentPriceHistoryRepository;
import com.bana.dotori.investment.repository.InvestmentProductRepository;
import com.bana.dotori.investment.repository.InvestmentTransactionRepository;
import com.bana.dotori.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvestmentProductServiceImpl implements InvestmentProductService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final InvestmentProductRepository investmentProductRepository;
	private final InvestmentPriceHistoryRepository investmentPriceHistoryRepository;
	private final InvestmentTransactionRepository investmentTransactionRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final InvestmentProductMapper investmentProductMapper;

	/**
	 * 상품·뉴스는 전역이지만 가격은 가족마다 다르다 — AI 가 가족별로
	 * investment_price_history 를 쌓으므로, 현재가는 (가족, 상품)의 최신 행이고
	 * 전일가는 오늘(KST) 자정 이전의 최신 행이다. 이력이 없으면 시작가(current_price)를 쓴다.
	 */
	@Override
	@Transactional(readOnly = true)
	public InvestmentProductListResponse getProducts(Integer userId, InvestmentProductCategory category) {
		Integer familyId = familyMemberRepository.findActiveFamilyId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		List<InvestmentProduct> products = category == null
			? investmentProductRepository.findAllByOrderByIdAsc()
			: investmentProductRepository.findAllByCategoryOrderByIdAsc(category);
		if (products.isEmpty()) {
			return new InvestmentProductListResponse(List.of());
		}

		// 현재가 = 오늘(KST) 하루가 끝나기 전까지의 최신 행, 전일가 = 오늘 자정 이전의 최신 행
		OffsetDateTime todayStart = LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();
		Map<Integer, Integer> latestPrices = latestPriceByProductId(familyId, todayStart.plusDays(1));
		Map<Integer, Integer> previousPrices = latestPriceByProductId(familyId, todayStart);

		return new InvestmentProductListResponse(products.stream()
			.map(product -> investmentProductMapper.toSummary(product,
				latestPrices.getOrDefault(product.getId(), product.getCurrentPrice()),
				previousPrices.getOrDefault(product.getId(), product.getCurrentPrice())))
			.toList());
	}

	/**
	 * 상세도 목록과 같은 가격 규칙이다. updatedAt(가격 갱신 일시)은 최신 가격 행의 기록 시각이고,
	 * 이력이 없으면 아직 시작가 그대로라는 뜻이라 상품 시드 시각(updated_at)을 내려 준다.
	 */
	@Override
	@Transactional(readOnly = true)
	public InvestmentProductDetailResponse getProductDetail(Integer userId, Integer productId) {
		Integer familyId = familyMemberRepository.findActiveFamilyId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		InvestmentProduct product = investmentProductRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

		OffsetDateTime todayStart = LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();
		Optional<InvestmentPriceHistory> latest = latestPriceRow(familyId, productId, todayStart.plusDays(1));
		Optional<InvestmentPriceHistory> previous = latestPriceRow(familyId, productId, todayStart);

		return investmentProductMapper.toDetailResponse(product,
			latest.map(InvestmentPriceHistory::getPrice).orElse(product.getCurrentPrice()),
			previous.map(InvestmentPriceHistory::getPrice).orElse(product.getCurrentPrice()),
			latest.map(InvestmentPriceHistory::getRecordedAt).orElse(product.getUpdatedAt()));
	}

	/**
	 * 보유 종목은 별도 테이블 없이 매수/매도 거래의 집계다 (명세 INV-T-03 각주) —
	 * 수량 = 매수 합 - 매도 합, 평균 매수 단가 = 매수 총액 ÷ 매수 총수량 (반올림).
	 * childId 를 주면 그 자녀의 보유를 조회한다 — 내 가족의 자녀가 아니면 403.
	 */
	@Override
	@Transactional(readOnly = true)
	public InvestmentHoldingsResponse getHoldings(Integer userId, Integer childId) {
		Integer familyId = familyMemberRepository.findActiveFamilyId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		Integer targetUserId = childId != null ? childId : userId;
		if (childId != null && !childId.equals(userId)) {
			FamilyMember target = familyMemberRepository.findWithUserByFamilyIdAndUserId(familyId, childId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FAMILY_CHILD));
			if (target.getUser().getRole() != UserRole.CHILD) {
				throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD);
			}
		}

		List<InvestmentTransaction> transactions = investmentTransactionRepository
			.findAllByFamilyIdAndChildId(familyId, targetUserId);

		Map<Integer, HoldingAccumulator> accumulators = new LinkedHashMap<>();
		for (InvestmentTransaction transaction : transactions) {
			accumulators.computeIfAbsent(transaction.getInvestmentProductId(), id -> new HoldingAccumulator())
				.add(transaction);
		}
		// 전량 매도한 종목(수량 0 이하)은 보유 목록에서 뺀다
		Map<Integer, HoldingAccumulator> held = accumulators.entrySet().stream()
			.filter(entry -> entry.getValue().quantity() > 0)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(first, second) -> first, LinkedHashMap::new));
		if (held.isEmpty()) {
			return new InvestmentHoldingsResponse(List.of());
		}

		Map<Integer, InvestmentProduct> products = investmentProductRepository.findAllById(held.keySet())
			.stream()
			.collect(Collectors.toMap(InvestmentProduct::getId, product -> product));
		OffsetDateTime todayStart = LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();
		Map<Integer, Integer> latestPrices = latestPriceByProductId(familyId, todayStart.plusDays(1));

		return new InvestmentHoldingsResponse(held.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> {
				InvestmentProduct product = products.get(entry.getKey());
				HoldingAccumulator accumulator = entry.getValue();
				return investmentProductMapper.toHoldingSummary(product, accumulator.quantity(),
					accumulator.averageBuyPrice(),
					latestPrices.getOrDefault(product.getId(), product.getCurrentPrice()));
			})
			.toList());
	}

	/** 한 종목의 매수/매도 누계. 수량과 평균 매수 단가를 거래에서 집계한다. */
	private static final class HoldingAccumulator {

		private long buyQuantity;
		private long sellQuantity;
		private long buyAmount;

		private void add(InvestmentTransaction transaction) {
			if (transaction.getTransactionType() == InvestmentTransactionType.BUY) {
				buyQuantity += transaction.getQuantity();
				buyAmount += transaction.getTotalAmount();
			} else {
				sellQuantity += transaction.getQuantity();
			}
		}

		private int quantity() {
			return (int)(buyQuantity - sellQuantity);
		}

		/** 매수 총액 ÷ 매수 총수량, 반올림. 매도는 평균 매수가를 바꾸지 않는다. */
		private int averageBuyPrice() {
			if (buyQuantity <= 0) {
				return 0;
			}
			return (int)Math.round((double)buyAmount / buyQuantity);
		}
	}

	private Map<Integer, Integer> latestPriceByProductId(Integer familyId, OffsetDateTime before) {
		return investmentPriceHistoryRepository.findLatestPerProductBefore(familyId, before).stream()
			.collect(Collectors.toMap(InvestmentPriceHistory::getInvestmentProductId,
				InvestmentPriceHistory::getPrice, (first, second) -> first));
	}

	private Optional<InvestmentPriceHistory> latestPriceRow(Integer familyId, Integer productId,
		OffsetDateTime before) {
		return investmentPriceHistoryRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				familyId, productId, before);
	}
}
