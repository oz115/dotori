package com.bana.dotori.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentHoldingSummary;
import com.bana.dotori.investment.dto.InvestmentHoldingsResponse;
import com.bana.dotori.investment.dto.InvestmentPriceHistoryResponse;
import com.bana.dotori.investment.dto.InvestmentTransactionListResponse;
import com.bana.dotori.investment.dto.InvestmentTransactionSummary;
import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.dto.InvestmentProductSummary;
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
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class InvestmentProductServiceImplTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final Integer USER_ID = 10;
	private static final Integer FAMILY_ID = 7;

	@Mock
	private InvestmentProductRepository investmentProductRepository;

	@Mock
	private InvestmentPriceHistoryRepository investmentPriceHistoryRepository;

	@Mock
	private InvestmentHoldingRepository investmentHoldingRepository;

	@Mock
	private InvestmentTransactionRepository investmentTransactionRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Spy
	private InvestmentProductMapper investmentProductMapper;

	@InjectMocks
	private InvestmentProductServiceImpl investmentProductService;

	private static final OffsetDateTime SEED_AT = OffsetDateTime.parse("2026-09-01T00:00:00+09:00");

	private InvestmentProduct product(Integer id, String name, int seedPrice) {
		InvestmentProduct product = InvestmentProduct.builder()
			.name(name)
			.description(name + " 회사")
			.category(InvestmentProductCategory.STOCK)
			.currentPrice(seedPrice)
			.isActive(true)
			.updatedAt(SEED_AT)
			.build();
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	private InvestmentPriceHistory history(Integer productId, int price, OffsetDateTime recordedAt) {
		return InvestmentPriceHistory.builder()
			.familyId(FAMILY_ID)
			.investmentProductId(productId)
			.price(price)
			.recordedAt(recordedAt)
			.build();
	}

	@Test
	@DisplayName("소속 가족이 없으면 FAMILY_NOT_FOUND 예외가 발생한다")
	void getProducts_noFamily() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> investmentProductService.getProducts(USER_ID, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("가격 이력이 없으면 시작가를 현재가로 쓰고 변동 포인트는 0이 된다")
	void getProducts_noHistory() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findAllByOrderByIdAsc())
			.willReturn(List.of(product(1, "시원아이스", 10000)));
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID),
			eq(todayStart().plusDays(1)))).willReturn(List.of());
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID), eq(todayStart())))
			.willReturn(List.of());

		InvestmentProductListResponse response = investmentProductService.getProducts(USER_ID, null);

		InvestmentProductSummary summary = response.products().get(0);
		assertThat(summary.investmentProductId()).isEqualTo(1);
		assertThat(summary.name()).isEqualTo("시원아이스");
		assertThat(summary.category()).isEqualTo(InvestmentProductCategory.STOCK);
		assertThat(summary.currentPrice()).isEqualTo(10000);
		assertThat(summary.changeAmount()).isZero();
		assertThat(summary.isActive()).isTrue();
	}

	@Test
	@DisplayName("오늘·전일 이력이 있으면 가족별 최신가와 전일 대비 변동 포인트를 계산한다")
	void getProducts_withHistory() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findAllByOrderByIdAsc())
			.willReturn(List.of(product(1, "시원아이스", 10000)));
		// 전일가 1000 → 현재가 1100: +100 포인트
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID),
			eq(todayStart().plusDays(1))))
			.willReturn(List.of(history(1, 1100, todayStart().plusHours(1))));
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID), eq(todayStart())))
			.willReturn(List.of(history(1, 1000, todayStart().minusHours(23))));

		InvestmentProductListResponse response = investmentProductService.getProducts(USER_ID, null);

		InvestmentProductSummary summary = response.products().get(0);
		assertThat(summary.currentPrice()).isEqualTo(1100);
		assertThat(summary.changeAmount()).isEqualTo(100);
	}

	@Test
	@DisplayName("오늘 이력만 있으면 시작가를 전일가로 보고 변동 포인트를 계산한다")
	void getProducts_onlyTodayHistory() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findAllByOrderByIdAsc())
			.willReturn(List.of(product(1, "시원아이스", 10000)));
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID),
			eq(todayStart().plusDays(1))))
			.willReturn(List.of(history(1, 9500, todayStart().plusHours(1))));
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID), eq(todayStart())))
			.willReturn(List.of());

		InvestmentProductListResponse response = investmentProductService.getProducts(USER_ID, null);

		InvestmentProductSummary summary = response.products().get(0);
		assertThat(summary.currentPrice()).isEqualTo(9500);
		// 시작가 10000 → 오늘 9500: -500 포인트
		assertThat(summary.changeAmount()).isEqualTo(-500);
	}

	@Test
	@DisplayName("카테고리 필터가 있으면 해당 카테고리 조회로 위임한다")
	void getProducts_categoryFilter() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findAllByCategoryOrderByIdAsc(InvestmentProductCategory.BOND))
			.willReturn(List.of());

		InvestmentProductListResponse response = investmentProductService.getProducts(USER_ID,
			InvestmentProductCategory.BOND);

		assertThat(response.products()).isEmpty();
		then(investmentProductRepository).should().findAllByCategoryOrderByIdAsc(InvestmentProductCategory.BOND);
		// 상품이 없으면 가격 이력을 조회하지 않는다
		then(investmentPriceHistoryRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("없는 상품을 상세 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void getProductDetail_notFound() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> investmentProductService.getProductDetail(USER_ID, 99))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("상세 조회에서 가격 이력이 없으면 시작가·변동 0·시드 시각을 내려 준다")
	void getProductDetail_noHistory() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, "시원아이스", 10000)));
		given(investmentPriceHistoryRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				eq(FAMILY_ID), eq(1), any(OffsetDateTime.class)))
			.willReturn(Optional.empty());

		InvestmentProductDetailResponse response = investmentProductService.getProductDetail(USER_ID, 1);

		assertThat(response.investmentProductId()).isEqualTo(1);
		assertThat(response.name()).isEqualTo("시원아이스");
		assertThat(response.description()).isEqualTo("시원아이스 회사");
		assertThat(response.currentPrice()).isEqualTo(10000);
		assertThat(response.previousPrice()).isEqualTo(10000);
		assertThat(response.changeAmount()).isZero();
		assertThat(response.isActive()).isTrue();
		assertThat(response.updatedAt()).isEqualTo(SEED_AT);
	}

	@Test
	@DisplayName("상세 조회에서 이력이 있으면 현재가·전일가·변동 포인트·가격 갱신 일시를 내려 준다")
	void getProductDetail_withHistory() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, "시원아이스", 10000)));
		OffsetDateTime recordedToday = todayStart().plusMinutes(5);
		given(investmentPriceHistoryRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				FAMILY_ID, 1, todayStart().plusDays(1)))
			.willReturn(Optional.of(history(1, 1100, recordedToday)));
		given(investmentPriceHistoryRepository
			.findFirstByFamilyIdAndInvestmentProductIdAndRecordedAtLessThanOrderByRecordedAtDescIdDesc(
				FAMILY_ID, 1, todayStart()))
			.willReturn(Optional.of(history(1, 1000, todayStart().minusHours(23).plusMinutes(55))));

		InvestmentProductDetailResponse response = investmentProductService.getProductDetail(USER_ID, 1);

		assertThat(response.currentPrice()).isEqualTo(1100);
		assertThat(response.previousPrice()).isEqualTo(1000);
		assertThat(response.changeAmount()).isEqualTo(100);
		assertThat(response.updatedAt()).isEqualTo(recordedToday);
	}

	private static final Integer CHILD_ID = 20;

	private InvestmentHolding holding(Integer childId, Integer productId, int quantity, int buyPrice) {
		return InvestmentHolding.builder()
			.familyId(FAMILY_ID)
			.childId(childId)
			.investmentProductId(productId)
			.quantity(quantity)
			.buyPrice(buyPrice)
			.build();
	}

	private FamilyMember familyChild() {
		return FamilyMember.builder()
			.user(User.builder().loginId("kid").name("아이").email("kid@test.local").role(UserRole.CHILD).build())
			.build();
	}

	@Test
	@DisplayName("childId가 내 가족의 자녀가 아니면 NOT_FAMILY_CHILD 예외가 발생한다")
	void getHoldings_childNotInFamily() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, CHILD_ID))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> investmentProductService.getHoldings(USER_ID, CHILD_ID))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NOT_FAMILY_CHILD);
	}

	@Test
	@DisplayName("childId가 자녀가 아니라 부모면 NOT_FAMILY_CHILD 예외가 발생한다")
	void getHoldings_targetIsParent() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		FamilyMember parentMember = FamilyMember.builder()
			.user(User.builder().loginId("mom").name("부모").email("mom@test.local").role(UserRole.PARENT).build())
			.build();
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, CHILD_ID))
			.willReturn(Optional.of(parentMember));

		assertThatThrownBy(() -> investmentProductService.getHoldings(USER_ID, CHILD_ID))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NOT_FAMILY_CHILD);
	}

	@Test
	@DisplayName("보유 행이 없으면 빈 보유 목록을 반환한다")
	void getHoldings_empty() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentHoldingRepository
			.findAllByFamilyIdAndChildIdOrderByInvestmentProductIdAsc(FAMILY_ID, USER_ID))
			.willReturn(List.of());

		InvestmentHoldingsResponse response = investmentProductService.getHoldings(USER_ID, null);

		assertThat(response.holdings()).isEmpty();
		// childId 없이 부르면 본인 보유라 자녀 검증을 하지 않는다
		then(familyMemberRepository).should(never()).findWithUserByFamilyIdAndUserId(eq(FAMILY_ID), any());
	}

	@Test
	@DisplayName("보유 행을 종목 정보·가족별 현재가와 합쳐 명세 형식으로 내려 준다")
	void getHoldings_withRows() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, CHILD_ID))
			.willReturn(Optional.of(familyChild()));
		given(investmentHoldingRepository
			.findAllByFamilyIdAndChildIdOrderByInvestmentProductIdAsc(FAMILY_ID, CHILD_ID))
			.willReturn(List.of(holding(CHILD_ID, 1, 15, 1100)));
		given(investmentProductRepository.findAllById(List.of(1)))
			.willReturn(List.of(product(1, "시원아이스", 10000)));
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID),
			eq(todayStart().plusDays(1))))
			.willReturn(List.of(history(1, 1300, todayStart().plusHours(1))));

		InvestmentHoldingsResponse response = investmentProductService.getHoldings(USER_ID, CHILD_ID);

		assertThat(response.holdings()).hasSize(1);
		InvestmentHoldingSummary holding = response.holdings().get(0);
		assertThat(holding.investmentProductId()).isEqualTo(1);
		assertThat(holding.productName()).isEqualTo("시원아이스");
		assertThat(holding.quantity()).isEqualTo(15);
		assertThat(holding.buyPrice()).isEqualTo(1100);
		assertThat(holding.currentPrice()).isEqualTo(1300);
	}

	@Test
	@DisplayName("보유 종목에 가격 이력이 없으면 현재가는 시작가가 된다")
	void getHoldings_noPriceHistory() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentHoldingRepository
			.findAllByFamilyIdAndChildIdOrderByInvestmentProductIdAsc(FAMILY_ID, USER_ID))
			.willReturn(List.of(holding(USER_ID, 1, 2, 10000)));
		given(investmentProductRepository.findAllById(List.of(1)))
			.willReturn(List.of(product(1, "시원아이스", 10000)));
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID),
			eq(todayStart().plusDays(1)))).willReturn(List.of());

		InvestmentHoldingsResponse response = investmentProductService.getHoldings(USER_ID, null);

		InvestmentHoldingSummary holding = response.holdings().get(0);
		assertThat(holding.quantity()).isEqualTo(2);
		assertThat(holding.buyPrice()).isEqualTo(10000);
		assertThat(holding.currentPrice()).isEqualTo(10000);
	}

	@Test
	@DisplayName("없는 상품의 가격 이력을 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void getPriceHistory_notFound() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> investmentProductService.getPriceHistory(USER_ID, 99, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("시작일이 종료일보다 늦으면 INVALID_DATE_RANGE 예외가 발생한다")
	void getPriceHistory_invalidRange() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, "시원아이스", 10000)));

		assertThatThrownBy(() -> investmentProductService.getPriceHistory(USER_ID, 1,
			LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_DATE_RANGE);
		then(investmentPriceHistoryRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("날짜를 안 주면 최근 30일 범위(KST 하루 경계)로 조회한다")
	void getPriceHistory_defaultRange() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, "시원아이스", 10000)));
		given(investmentPriceHistoryRepository.findAllInRange(eq(FAMILY_ID), eq(1), any(OffsetDateTime.class),
			any(OffsetDateTime.class))).willReturn(List.of());

		InvestmentPriceHistoryResponse response = investmentProductService.getPriceHistory(USER_ID, 1, null,
			null);

		assertThat(response.investmentProductId()).isEqualTo(1);
		assertThat(response.history()).isEmpty();
		then(investmentPriceHistoryRepository).should().findAllInRange(FAMILY_ID, 1,
			todayStart().minusDays(30), todayStart().plusDays(1));
	}

	@Test
	@DisplayName("기간을 주면 그 범위의 이력을 시간 오름차순 그대로 한국 시간으로 내려 준다")
	void getPriceHistory_withDates() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findById(1)).willReturn(Optional.of(product(1, "시원아이스", 10000)));
		OffsetDateTime start = LocalDate.of(2026, 9, 1).atStartOfDay(KST).toOffsetDateTime();
		OffsetDateTime end = LocalDate.of(2026, 9, 16).atStartOfDay(KST).toOffsetDateTime();
		given(investmentPriceHistoryRepository.findAllInRange(FAMILY_ID, 1, start, end)).willReturn(List.of(
			history(1, 1150, OffsetDateTime.parse("2026-09-14T00:05:00+09:00")),
			history(1, 1200, OffsetDateTime.parse("2026-09-15T00:05:00+09:00"))));

		InvestmentPriceHistoryResponse response = investmentProductService.getPriceHistory(USER_ID, 1,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15));

		assertThat(response.history()).hasSize(2);
		assertThat(response.history().get(0).price()).isEqualTo(1150);
		assertThat(response.history().get(0).recordedAt())
			.isEqualTo(OffsetDateTime.parse("2026-09-14T00:05:00+09:00"));
		assertThat(response.history().get(1).price()).isEqualTo(1200);
	}

	private InvestmentTransaction transaction(Integer id, Integer productId, InvestmentTransactionType type,
		int unitPrice, OffsetDateTime createdAt) {
		InvestmentTransaction transaction = InvestmentTransaction.builder()
			.familyId(FAMILY_ID)
			.childId(USER_ID)
			.investmentProductId(productId)
			.transactionType(type)
			.quantity(1)
			.unitPrice(unitPrice)
			.totalAmount(unitPrice)
			.createdAt(createdAt)
			.build();
		ReflectionTestUtils.setField(transaction, "id", id);
		return transaction;
	}

	@Test
	@DisplayName("거래 이력에서 childId가 내 가족의 자녀가 아니면 NOT_FAMILY_CHILD 예외가 발생한다")
	void getTransactions_childNotInFamily() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(FAMILY_ID, CHILD_ID))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> investmentProductService.getTransactions(USER_ID, CHILD_ID, null, null, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NOT_FAMILY_CHILD);
	}

	@Test
	@DisplayName("거래 이력에서 시작일이 종료일보다 늦으면 INVALID_DATE_RANGE 예외가 발생한다")
	void getTransactions_invalidRange() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));

		assertThatThrownBy(() -> investmentProductService.getTransactions(USER_ID, null,
			LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1), null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_DATE_RANGE);
	}

	@Test
	@DisplayName("거래 이력을 최신순으로 종목명과 함께 내려 주고, 없으면 빈 목록이다")
	void getTransactions_success() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentTransactionRepository.findAllInRange(eq(FAMILY_ID), eq(USER_ID),
			any(OffsetDateTime.class), any(OffsetDateTime.class)))
			.willReturn(List.of(
				transaction(2, 1, InvestmentTransactionType.SELL, 1300,
					OffsetDateTime.parse("2026-09-15T10:00:00+09:00")),
				transaction(1, 1, InvestmentTransactionType.BUY, 1000,
					OffsetDateTime.parse("2026-09-10T10:00:00+09:00"))));
		given(investmentProductRepository.findAllById(List.of(1)))
			.willReturn(List.of(product(1, "시원아이스", 10000)));

		InvestmentTransactionListResponse response = investmentProductService.getTransactions(USER_ID, null,
			null, null, null);

		assertThat(response.transactions()).hasSize(2);
		InvestmentTransactionSummary latest = response.transactions().get(0);
		assertThat(latest.investmentTransactionId()).isEqualTo(2);
		assertThat(latest.productName()).isEqualTo("시원아이스");
		assertThat(latest.transactionType()).isEqualTo(InvestmentTransactionType.SELL);
		assertThat(latest.quantity()).isEqualTo(1);
		assertThat(latest.unitPrice()).isEqualTo(1300);
		assertThat(latest.totalAmount()).isEqualTo(1300);
		assertThat(latest.createdAt()).isEqualTo(OffsetDateTime.parse("2026-09-15T10:00:00+09:00"));
	}

	@Test
	@DisplayName("type 필터가 있으면 유형별 조회로 위임하고, 결과가 없으면 상품을 조회하지 않는다")
	void getTransactions_typeFilter() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentTransactionRepository.findAllInRangeByType(eq(FAMILY_ID), eq(USER_ID),
			eq(InvestmentTransactionType.BUY), any(OffsetDateTime.class), any(OffsetDateTime.class)))
			.willReturn(List.of());

		InvestmentTransactionListResponse response = investmentProductService.getTransactions(USER_ID, null,
			null, null, InvestmentTransactionType.BUY);

		assertThat(response.transactions()).isEmpty();
		then(investmentProductRepository).shouldHaveNoInteractions();
	}

	private OffsetDateTime todayStart() {
		return LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();
	}
}
