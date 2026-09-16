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

import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.entity.InvestmentPriceHistory;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.mapper.InvestmentProductMapper;
import com.bana.dotori.investment.repository.InvestmentPriceHistoryRepository;
import com.bana.dotori.investment.repository.InvestmentProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvestmentProductServiceImpl implements InvestmentProductService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final InvestmentProductRepository investmentProductRepository;
	private final InvestmentPriceHistoryRepository investmentPriceHistoryRepository;
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
