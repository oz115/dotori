package com.bana.dotori.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.investment.dto.InvestmentNewsDetailResponse;
import com.bana.dotori.investment.dto.InvestmentNewsListResponse;
import com.bana.dotori.investment.entity.InvestmentNews;
import com.bana.dotori.investment.entity.InvestmentNewsImpact;
import com.bana.dotori.investment.entity.InvestmentNewsProduct;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.mapper.InvestmentNewsMapper;
import com.bana.dotori.investment.repository.InvestmentNewsProductRepository;
import com.bana.dotori.investment.repository.InvestmentNewsRepository;

@ExtendWith(MockitoExtension.class)
class InvestmentNewsServiceImplTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Mock
	private InvestmentNewsRepository investmentNewsRepository;

	@Mock
	private InvestmentNewsProductRepository investmentNewsProductRepository;

	@Spy
	private InvestmentNewsMapper investmentNewsMapper;

	@InjectMocks
	private InvestmentNewsServiceImpl investmentNewsService;

	private InvestmentNews news(Integer id, String title) {
		InvestmentNews news = InvestmentNews.builder()
			.title(title)
			.content(title + " 본문")
			.impact(InvestmentNewsImpact.POSITIVE)
			.publishedAt(OffsetDateTime.parse("2026-08-28T06:00:00+09:00"))
			.build();
		ReflectionTestUtils.setField(news, "id", id);
		return news;
	}

	private InvestmentNewsProduct mapping(InvestmentNews news, Integer productId, String productName) {
		InvestmentProduct product = InvestmentProduct.builder()
			.name(productName)
			.category(InvestmentProductCategory.STOCK)
			.build();
		ReflectionTestUtils.setField(product, "id", productId);
		return InvestmentNewsProduct.builder().news(news).product(product).build();
	}

	@Test
	@DisplayName("날짜를 주면 한국 시간 기준 그날 하루 범위로 조회하고 뉴스별 관련 종목을 묶어 준다")
	void getNews_byDate() {
		InvestmentNews first = news(1, "폭염으로 팥빙수 수요 급증!");
		InvestmentNews second = news(2, "로봇 공장 증설");
		given(investmentNewsRepository.findAllPublishedBetween(any(), any()))
			.willReturn(List.of(first, second));
		given(investmentNewsProductRepository.findAllWithProductByNewsIds(anyCollection()))
			.willReturn(List.of(mapping(first, 1, "팥빙수 회사"), mapping(second, 2, "로봇 회사"),
				mapping(second, 3, "부품 회사")));

		InvestmentNewsListResponse response = investmentNewsService.getNews(LocalDate.of(2026, 8, 28), null);

		ArgumentCaptor<OffsetDateTime> start = ArgumentCaptor.forClass(OffsetDateTime.class);
		ArgumentCaptor<OffsetDateTime> end = ArgumentCaptor.forClass(OffsetDateTime.class);
		verify(investmentNewsRepository).findAllPublishedBetween(start.capture(), end.capture());
		assertThat(start.getValue()).isEqualTo(LocalDate.of(2026, 8, 28).atStartOfDay(KST).toOffsetDateTime());
		assertThat(end.getValue()).isEqualTo(start.getValue().plusDays(1));

		assertThat(response.news()).hasSize(2);
		assertThat(response.news().get(0).title()).isEqualTo("폭염으로 팥빙수 수요 급증!");
		assertThat(response.news().get(0).impact()).isEqualTo(InvestmentNewsImpact.POSITIVE);
		assertThat(response.news().get(0).relatedProducts()).hasSize(1);
		assertThat(response.news().get(0).relatedProducts().get(0).productName()).isEqualTo("팥빙수 회사");
		assertThat(response.news().get(1).relatedProducts()).hasSize(2);
	}

	@Test
	@DisplayName("날짜가 없으면 오늘(한국 시간)로 조회한다")
	void getNews_defaultsToToday() {
		given(investmentNewsRepository.findAllPublishedBetween(any(), any())).willReturn(List.of());

		investmentNewsService.getNews(null, null);

		ArgumentCaptor<OffsetDateTime> start = ArgumentCaptor.forClass(OffsetDateTime.class);
		verify(investmentNewsRepository).findAllPublishedBetween(start.capture(), any());
		assertThat(start.getValue()).isEqualTo(LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime());
	}

	@Test
	@DisplayName("productId 를 주면 그 종목과 연결된 뉴스만 조회하는 쿼리를 탄다")
	void getNews_byProductId() {
		given(investmentNewsRepository.findAllPublishedBetweenByProductId(any(), any(), eq(7)))
			.willReturn(List.of());

		investmentNewsService.getNews(LocalDate.of(2026, 8, 28), 7);

		then(investmentNewsRepository).should()
			.findAllPublishedBetweenByProductId(any(), any(), eq(7));
		then(investmentNewsProductRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("상세 조회는 본문과 관련 종목, 한국 시간 발행 일시를 내려 준다")
	void getNewsDetail_success() {
		InvestmentNews news = news(1, "폭염으로 팥빙수 수요 급증!");
		given(investmentNewsRepository.findById(1)).willReturn(Optional.of(news));
		given(investmentNewsProductRepository.findAllWithProductByNewsIds(List.of(1)))
			.willReturn(List.of(mapping(news, 1, "팥빙수 회사")));

		InvestmentNewsDetailResponse response = investmentNewsService.getNewsDetail(1);

		assertThat(response.investmentNewsId()).isEqualTo(1);
		assertThat(response.content()).isEqualTo("폭염으로 팥빙수 수요 급증! 본문");
		assertThat(response.relatedProducts().get(0).investmentProductId()).isEqualTo(1);
		assertThat(response.relatedProducts().get(0).category())
			.isEqualTo(InvestmentProductCategory.STOCK);
		assertThat(response.publishedAt().getOffset().getTotalSeconds()).isEqualTo(9 * 3600);
	}

	@Test
	@DisplayName("없는 뉴스를 상세 조회하면 NEWS_NOT_FOUND 예외가 발생한다")
	void getNewsDetail_notFound() {
		given(investmentNewsRepository.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> investmentNewsService.getNewsDetail(99))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NEWS_NOT_FOUND);
	}
}
