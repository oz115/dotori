package com.bana.dotori.investment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.investment.dto.InvestmentNewsDetailResponse;
import com.bana.dotori.investment.dto.InvestmentNewsListResponse;
import com.bana.dotori.investment.dto.InvestmentNewsSummary;
import com.bana.dotori.investment.dto.RelatedProductResponse;
import com.bana.dotori.investment.entity.InvestmentNewsImpact;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.service.InvestmentNewsService;

@WebMvcTest(InvestmentNewsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class InvestmentNewsControllerTest {

	private static final String NEWS_URL = "/api/v1/investments/news";
	private static final String BEARER = "Bearer valid-access-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InvestmentNewsService investmentNewsService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication("valid-access-token"))
			.willReturn(new UsernamePasswordAuthenticationToken(1, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	private InvestmentNewsSummary summary() {
		return new InvestmentNewsSummary(1, "폭염으로 팥빙수 수요 급증!", InvestmentNewsImpact.POSITIVE,
			List.of(new RelatedProductResponse(1, "팥빙수 회사", InvestmentProductCategory.STOCK)),
			OffsetDateTime.parse("2026-08-28T06:00:00+09:00"));
	}

	@Test
	@DisplayName("뉴스 목록 조회 성공 시 200과 명세 형식의 목록을 반환한다")
	void getNews_success() throws Exception {
		givenAuthenticated();
		given(investmentNewsService.getNews(any(), any()))
			.willReturn(new InvestmentNewsListResponse(List.of(summary())));

		mockMvc.perform(get(NEWS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.news[0].investmentNewsId").value(1))
			.andExpect(jsonPath("$.news[0].title").value("폭염으로 팥빙수 수요 급증!"))
			.andExpect(jsonPath("$.news[0].impact").value("POSITIVE"))
			.andExpect(jsonPath("$.news[0].relatedProducts[0].productName").value("팥빙수 회사"))
			.andExpect(jsonPath("$.news[0].publishedAt").exists());
	}

	@Test
	@DisplayName("date·productId 쿼리 파라미터를 서비스에 그대로 전달한다")
	void getNews_withFilters() throws Exception {
		givenAuthenticated();
		given(investmentNewsService.getNews(any(), any()))
			.willReturn(new InvestmentNewsListResponse(List.of()));

		mockMvc.perform(get(NEWS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("date", "2026-08-28")
				.param("productId", "1"))
			.andExpect(status().isOk());

		then(investmentNewsService).should().getNews(eq(LocalDate.of(2026, 8, 28)), eq(1));
	}

	@Test
	@DisplayName("date 형식이 올바르지 않으면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void getNews_invalidDate() throws Exception {
		givenAuthenticated();

		mockMvc.perform(get(NEWS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("date", "2026-13-99"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(investmentNewsService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("뉴스 상세 조회 성공 시 200과 본문을 반환한다")
	void getNewsDetail_success() throws Exception {
		givenAuthenticated();
		given(investmentNewsService.getNewsDetail(1)).willReturn(new InvestmentNewsDetailResponse(
			1, "폭염으로 팥빙수 수요 급증!", "연일 계속되는 폭염으로 팥빙수 회사의 매출이...",
			InvestmentNewsImpact.POSITIVE,
			List.of(new RelatedProductResponse(1, "팥빙수 회사", InvestmentProductCategory.STOCK)),
			OffsetDateTime.parse("2026-08-28T06:00:00+09:00")));

		mockMvc.perform(get(NEWS_URL + "/1").header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.investmentNewsId").value(1))
			.andExpect(jsonPath("$.content").value("연일 계속되는 폭염으로 팥빙수 회사의 매출이..."))
			.andExpect(jsonPath("$.impact").value("POSITIVE"));
	}

	@Test
	@DisplayName("없는 뉴스를 조회하면 404와 NEWS_NOT_FOUND 에러를 반환한다")
	void getNewsDetail_notFound() throws Exception {
		givenAuthenticated();
		given(investmentNewsService.getNewsDetail(99))
			.willThrow(new BusinessException(ErrorCode.NEWS_NOT_FOUND));

		mockMvc.perform(get(NEWS_URL + "/99").header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NEWS_NOT_FOUND"));
	}

	@Test
	@DisplayName("토큰 없이 호출하면 401과 UNAUTHORIZED 에러를 반환한다")
	void getNews_unauthorized() throws Exception {
		mockMvc.perform(get(NEWS_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(investmentNewsService).shouldHaveNoInteractions();
	}
}
