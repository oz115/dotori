package com.bana.dotori.investment.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import com.bana.dotori.investment.dto.InvestmentPriceHistoryResponse;
import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.dto.InvestmentTransactionListResponse;
import com.bana.dotori.investment.entity.InvestmentHolding;
import com.bana.dotori.investment.entity.InvestmentPriceHistory;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.entity.InvestmentTransaction;
import com.bana.dotori.investment.entity.InvestmentTransactionType;
import com.bana.dotori.investment.mapper.InvestmentProductMapper;
import com.bana.dotori.investment.repository.InvestmentHoldingRepository;
import com.bana.dotori.investment.repository.InvestmentPriceHistoryRepository;
import com.bana.dotori.investment.repository.InvestmentProductRepository;
import com.bana.dotori.investment.repository.InvestmentTransactionRepository;
import com.bana.dotori.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvestmentProductServiceImpl implements InvestmentProductService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	// 거래 이력 조회에서 시작일을 안 줬을 때의 하한 — 서비스 오픈 이전이라 사실상 전체 조회다
	private static final LocalDate EARLIEST_DATE = LocalDate.of(2000, 1, 1);

	private final InvestmentProductRepository investmentProductRepository;
	private final InvestmentPriceHistoryRepository investmentPriceHistoryRepository;
	private final InvestmentHoldingRepository investmentHoldingRepository;
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
	 * 보유는 investment_holdings 에서 읽는다 — 매수/매도 API 가 거래 기록과 같은
	 * 트랜잭션으로 갱신해 두는 아이·종목당 한 행이다. childId 를 주면 그 자녀의 보유를
	 * 조회한다 (부모용) — 내 가족의 자녀가 아니면 403.
	 */
	@Override
	@Transactional(readOnly = true)
	public InvestmentHoldingsResponse getHoldings(Integer userId, Integer childId) {
		Integer familyId = familyMemberRepository.findActiveFamilyId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		Integer targetUserId = resolveTargetChild(familyId, userId, childId);

		List<InvestmentHolding> holdings = investmentHoldingRepository
			.findAllByFamilyIdAndChildIdOrderByInvestmentProductIdAsc(familyId, targetUserId);
		if (holdings.isEmpty()) {
			return new InvestmentHoldingsResponse(List.of());
		}

		Map<Integer, InvestmentProduct> products = investmentProductRepository
			.findAllById(holdings.stream().map(InvestmentHolding::getInvestmentProductId).toList())
			.stream()
			.collect(Collectors.toMap(InvestmentProduct::getId, product -> product));
		OffsetDateTime todayStart = LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();
		Map<Integer, Integer> latestPrices = latestPriceByProductId(familyId, todayStart.plusDays(1));

		return new InvestmentHoldingsResponse(holdings.stream()
			.map(holding -> {
				InvestmentProduct product = products.get(holding.getInvestmentProductId());
				return investmentProductMapper.toHoldingSummary(product, holding.getQuantity(),
					holding.getBuyPrice(),
					latestPrices.getOrDefault(product.getId(), product.getCurrentPrice()));
			})
			.toList());
	}

	/**
	 * 상세 화면 차트용 가격 이력 (INV-T-06). 가격이 가족별이라 조회한 사용자의 가족 기준이고,
	 * 기간 기본값은 명세대로 최근 30일 [30일 전, 오늘]이다. AI 가 아직 가격을 안 움직인
	 * 기간은 행이 없어 빈 배열이 나갈 수 있다 — 프론트는 그때 시작가 한 점으로 그린다.
	 */
	@Override
	@Transactional(readOnly = true)
	public InvestmentPriceHistoryResponse getPriceHistory(Integer userId, Integer productId, LocalDate startDate,
		LocalDate endDate) {
		Integer familyId = familyMemberRepository.findActiveFamilyId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		InvestmentProduct product = investmentProductRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

		LocalDate end = endDate != null ? endDate : LocalDate.now(KST);
		LocalDate start = startDate != null ? startDate : end.minusDays(30);
		if (start.isAfter(end)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
		}

		// recorded_at 이 TIMESTAMPTZ 라 날짜 동등 비교 대신 KST 하루 경계 범위로 거른다 (뉴스와 같은 방식)
		List<InvestmentPriceHistory> rows = investmentPriceHistoryRepository.findAllInRange(familyId,
			product.getId(), start.atStartOfDay(KST).toOffsetDateTime(),
			end.plusDays(1).atStartOfDay(KST).toOffsetDateTime());
		return investmentProductMapper.toPriceHistoryResponse(product.getId(), rows);
	}

	/**
	 * 거래 이력 조회 (INV-T-05). 보유 조회와 같은 접근 규칙 — childId 없으면 본인,
	 * 있으면 내 가족 자녀(부모용). 기간을 안 주면 전체(시작일)와 오늘(종료일)이 기본이다.
	 */
	@Override
	@Transactional(readOnly = true)
	public InvestmentTransactionListResponse getTransactions(Integer userId, Integer childId,
		LocalDate startDate, LocalDate endDate, InvestmentTransactionType type) {
		Integer familyId = familyMemberRepository.findActiveFamilyId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		Integer targetUserId = resolveTargetChild(familyId, userId, childId);

		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
		}
		// created_at 이 TIMESTAMPTZ 라 KST 하루 경계 범위로 거른다. 시작일 기본은 서비스
		// 이전의 고정 날짜(사실상 전체), 종료일 기본은 오늘.
		OffsetDateTime start = (startDate != null ? startDate : EARLIEST_DATE)
			.atStartOfDay(KST).toOffsetDateTime();
		OffsetDateTime end = (endDate != null ? endDate : LocalDate.now(KST))
			.plusDays(1).atStartOfDay(KST).toOffsetDateTime();

		List<InvestmentTransaction> transactions = type == null
			? investmentTransactionRepository.findAllInRange(familyId, targetUserId, start, end)
			: investmentTransactionRepository.findAllInRangeByType(familyId, targetUserId, type, start, end);
		if (transactions.isEmpty()) {
			return new InvestmentTransactionListResponse(List.of());
		}

		Map<Integer, InvestmentProduct> products = investmentProductRepository
			.findAllById(transactions.stream()
				.map(InvestmentTransaction::getInvestmentProductId)
				.distinct()
				.toList())
			.stream()
			.collect(Collectors.toMap(InvestmentProduct::getId, product -> product));

		return new InvestmentTransactionListResponse(transactions.stream()
			.map(transaction -> investmentProductMapper.toTransactionSummary(transaction,
				products.get(transaction.getInvestmentProductId()).getName()))
			.toList());
	}

	/**
	 * childId 없으면 본인, 있으면 내 가족의 자녀인지 확인하고 그 아이를 대상으로 한다.
	 * 남의 집 아이거나 같은 집 부모면 403 NOT_FAMILY_CHILD (보유·거래 이력이 공유).
	 */
	private Integer resolveTargetChild(Integer familyId, Integer userId, Integer childId) {
		if (childId == null || childId.equals(userId)) {
			return userId;
		}
		FamilyMember target = familyMemberRepository.findWithUserByFamilyIdAndUserId(familyId, childId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FAMILY_CHILD));
		if (target.getUser().getRole() != UserRole.CHILD) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD);
		}
		return childId;
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
