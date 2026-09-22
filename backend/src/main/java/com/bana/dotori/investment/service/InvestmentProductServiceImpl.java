package com.bana.dotori.investment.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentHoldingsResponse;
import com.bana.dotori.investment.dto.InvestmentPriceHistoryResponse;
import com.bana.dotori.investment.dto.InvestmentPricePoint;
import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.dto.InvestmentTransactionListResponse;
import com.bana.dotori.investment.dto.InvestmentTransactionSummary;
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
	// 종목 수. 노션 「회사 리스트」가 10개로 고정했다 — 시작가(용돈÷종목 수) 계산에 쓴다
	private static final int STOCK_COUNT = 10;

	private final InvestmentProductRepository investmentProductRepository;
	private final InvestmentPriceHistoryRepository investmentPriceHistoryRepository;
	private final InvestmentHoldingRepository investmentHoldingRepository;
	private final InvestmentTransactionRepository investmentTransactionRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final FamilyRepository familyRepository;
	private final InvestmentProductMapper investmentProductMapper;

	/**
	 * 상품·뉴스는 전역이지만 가격은 가족마다 다르다 — AI 가 가족별로
	 * investment_price_history 를 쌓으므로, 현재가는 (가족, 상품)의 최신 행이고
	 * 전일가는 오늘(KST) 자정 이전의 최신 행이다. 이력이 없으면 가족별 시작가를 쓴다
	 * ({@link #familyStartPrice}).
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
		Integer familyStartPrice = familyStartPrice(familyId);

		return new InvestmentProductListResponse(products.stream()
			.map(product -> investmentProductMapper.toSummary(product,
				latestPrices.getOrDefault(product.getId(), startPrice(familyStartPrice, product)),
				previousPrices.getOrDefault(product.getId(), startPrice(familyStartPrice, product))))
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
		Integer familyStartPrice = familyStartPrice(familyId);

		return investmentProductMapper.toDetailResponse(product,
			latest.map(InvestmentPriceHistory::getPrice).orElseGet(() -> startPrice(familyStartPrice, product)),
			previous.map(InvestmentPriceHistory::getPrice).orElseGet(() -> startPrice(familyStartPrice, product)),
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
		Integer familyStartPrice = familyStartPrice(familyId);

		return new InvestmentHoldingsResponse(holdings.stream()
			.map(holding -> {
				InvestmentProduct product = products.get(holding.getInvestmentProductId());
				return investmentProductMapper.toHoldingSummary(product, holding.getQuantity(),
					holding.getBuyPrice(),
					latestPrices.getOrDefault(product.getId(), startPrice(familyStartPrice, product)));
			})
			.toList());
	}

	/**
	 * 상세 화면 차트용 가격 이력 (INV-T-06). 가격이 가족별이라 조회한 사용자의 가족 기준이고,
	 * 기간 기본값은 최근 30일 [30일 전, 오늘]이다. 변동 없는 날짜는 직전 가격으로
	 * 채우고 최초 기록 전에는 시작가를 쓴다. 가족·상품 생성 전과 미래는 제외한다.
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

		// 아직 존재하지 않은 날짜와 미래 가격은 만들지 않는다. 왼쪽 끝은 가입일이 아니라
		// "이 가족의 첫 가격 기록"이다 — 온보딩 때 지난 일주일치를 백필해 주므로(AI),
		// 어제 가입한 아이도 일주일 흐름을 본다. 기록이 아직 없으면 오늘 한 점(시작가)만 만든다.
		LocalDate firstRecorded = investmentPriceHistoryRepository
			.findFirstByFamilyIdAndInvestmentProductIdOrderByRecordedAtAscIdAsc(familyId, product.getId())
			.map(row -> row.getRecordedAt().atZoneSameInstant(KST).toLocalDate())
			.orElseGet(() -> LocalDate.now(KST));
		LocalDate productCreated = product.getCreatedAt().atZoneSameInstant(KST).toLocalDate();
		start = start.isBefore(firstRecorded) ? firstRecorded : start;
		start = start.isBefore(productCreated) ? productCreated : start;
		LocalDate today = LocalDate.now(KST);
		end = end.isAfter(today) ? today : end;
		if (start.isAfter(end)) {
			return new InvestmentPriceHistoryResponse(productId, List.of());
		}

		// 기간 이전 최신가를 이어 받는다. 이력이 전혀 없으면 가족별 시작가가 실제 가격이다.
		Integer familyStartPrice = familyStartPrice(familyId);
		int price = latestPriceRow(familyId, productId, start.atStartOfDay(KST).toOffsetDateTime())
			.map(InvestmentPriceHistory::getPrice).orElseGet(() -> startPrice(familyStartPrice, product));
		List<InvestmentPriceHistory> rows = investmentPriceHistoryRepository.findAllInRange(familyId,
			product.getId(), start.atStartOfDay(KST).toOffsetDateTime(),
			end.plusDays(1).atStartOfDay(KST).toOffsetDateTime());
		List<InvestmentPricePoint> points = new ArrayList<>();
		int index = 0;
		for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
			while (index < rows.size()
				&& !rows.get(index).getRecordedAt().atZoneSameInstant(KST).toLocalDate().isAfter(day)) {
				price = rows.get(index++).getPrice();
			}
			// recordedAt은 이 응답에서 일별 가격 기준일(KST 00:00)을 나타낸다.
			points.add(new InvestmentPricePoint(price, day.atStartOfDay(KST).toOffsetDateTime()));
		}
		return new InvestmentPriceHistoryResponse(productId, points);
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

		// 기간·유형 필터 밖의 매수도 포함해야 매도의 실제 원가를 찾을 수 있다.
		List<InvestmentTransaction> all = type == null && startDate == null ? transactions
			: investmentTransactionRepository.findAllInRange(familyId, targetUserId,
				EARLIEST_DATE.atStartOfDay(KST).toOffsetDateTime(), end);
		Map<Integer, Integer> openBuys = new HashMap<>();
		Map<Integer, Integer> buyPrices = new HashMap<>();
		all.stream().sorted(Comparator.comparing(InvestmentTransaction::getCreatedAt)
			.thenComparing(InvestmentTransaction::getId)).forEach(transaction -> {
				if (transaction.getTransactionType() == InvestmentTransactionType.BUY) {
					openBuys.put(transaction.getInvestmentProductId(), transaction.getUnitPrice());
				} else {
					Integer buyPrice = openBuys.remove(transaction.getInvestmentProductId());
					if (buyPrice != null) {
						buyPrices.put(transaction.getId(), buyPrice);
					}
				}
			});
		return new InvestmentTransactionListResponse(transactions.stream()
			.map(transaction -> {
				InvestmentTransactionSummary row = investmentProductMapper.toTransactionSummary(transaction,
					products.get(transaction.getInvestmentProductId()).getName());
				Integer buyPrice = buyPrices.get(transaction.getId());
				Integer profit = buyPrice == null ? null : (row.unitPrice() - buyPrice) * row.quantity();
				return new InvestmentTransactionSummary(row.investmentTransactionId(), row.productName(),
					row.transactionType(), row.quantity(), row.unitPrice(), row.totalAmount(), row.createdAt(),
					buyPrice, profit);
			})
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

	/**
	 * 이 가족의 시작가 — 가격 이력이 아직 없을 때 쓰는 가격. 시드 current_price(전 가족 공통
	 * 자리표시자)가 아니라 <b>주간 기본 용돈(weekly_allowance, 포인트) ÷ 종목 수(10)</b>다.
	 * 아이가 일주일 용돈으로 10개 종목을 전부 살 수 있는 가격이고, 그렇게 다 사서 일주일
	 * (뉴스 21건 = 상승 14·하락 7) 들고 팔아도 최대 이익이 투자 배정액(한도의 40%)을 넘지
	 * 않는다 — AI 의 하루 변동폭(한도×40%÷7)과 짝을 이루는 설계다.
	 * 온보딩 전(용돈 없음·0)이면 null 을 돌려주고 상품 시드 값으로 폴백한다.
	 */
	private Integer familyStartPrice(Integer familyId) {
		return familyRepository.findByIdAndDeletedAtIsNull(familyId)
			.map(Family::getWeeklyAllowance)
			.filter(allowance -> allowance > 0)
			.map(allowance -> Math.max(1, (allowance + STOCK_COUNT / 2) / STOCK_COUNT))
			.orElse(null);
	}

	private static int startPrice(Integer familyStartPrice, InvestmentProduct product) {
		return familyStartPrice != null ? familyStartPrice : product.getCurrentPrice();
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
