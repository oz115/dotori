package com.bana.dotori.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.dto.InvestmentProductSummary;
import com.bana.dotori.investment.entity.InvestmentPriceHistory;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.mapper.InvestmentProductMapper;
import com.bana.dotori.investment.repository.InvestmentPriceHistoryRepository;
import com.bana.dotori.investment.repository.InvestmentProductRepository;

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
	private FamilyMemberRepository familyMemberRepository;

	@Spy
	private InvestmentProductMapper investmentProductMapper;

	@InjectMocks
	private InvestmentProductServiceImpl investmentProductService;

	private InvestmentProduct product(Integer id, String name, int seedPrice) {
		InvestmentProduct product = InvestmentProduct.builder()
			.name(name)
			.category(InvestmentProductCategory.STOCK)
			.currentPrice(seedPrice)
			.isActive(true)
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
	@DisplayName("가격 이력이 없으면 시작가를 현재가로 쓰고 등락률은 0이 된다")
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
		assertThat(summary.changeRate()).isEqualTo(0.0);
		assertThat(summary.isActive()).isTrue();
	}

	@Test
	@DisplayName("오늘·전일 이력이 있으면 가족별 최신가와 전일 대비 등락률을 계산한다")
	void getProducts_withHistory() {
		given(familyMemberRepository.findActiveFamilyId(USER_ID)).willReturn(Optional.of(FAMILY_ID));
		given(investmentProductRepository.findAllByOrderByIdAsc())
			.willReturn(List.of(product(1, "시원아이스", 10000)));
		// 전일가 1150 → 현재가 1200: 50 / 1150 * 100 = 4.347... → 4.35 (명세 예시와 같은 형식)
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID),
			eq(todayStart().plusDays(1))))
			.willReturn(List.of(history(1, 1200, todayStart().plusHours(1))));
		given(investmentPriceHistoryRepository.findLatestPerProductBefore(eq(FAMILY_ID), eq(todayStart())))
			.willReturn(List.of(history(1, 1150, todayStart().minusHours(23))));

		InvestmentProductListResponse response = investmentProductService.getProducts(USER_ID, null);

		InvestmentProductSummary summary = response.products().get(0);
		assertThat(summary.currentPrice()).isEqualTo(1200);
		assertThat(summary.changeRate()).isEqualTo(4.35);
	}

	@Test
	@DisplayName("오늘 이력만 있으면 시작가를 전일가로 보고 등락률을 계산한다")
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
		// (9500 - 10000) / 10000 * 100 = -5.00
		assertThat(summary.changeRate()).isEqualTo(-5.0);
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

	private OffsetDateTime todayStart() {
		return LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();
	}
}
