package com.bana.dotori.investment.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

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
import com.bana.dotori.investment.dto.InvestmentNewsReviewResponse;
import com.bana.dotori.investment.dto.InvestmentNewsReviewResponse.Review;
import com.bana.dotori.investment.service.InvestmentNewsReviewService;

@WebMvcTest(InvestmentNewsReviewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class InvestmentNewsReviewControllerTest {

	private static final String NEWS_URL = "/api/v1/investments/news/review";
	private static final String BEARER = "Bearer valid-access-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InvestmentNewsReviewService reviewService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication("valid-access-token"))
			.willReturn(new UsernamePasswordAuthenticationToken(1, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	@Test
	void getReview_success() throws Exception {
		givenAuthenticated();
		given(reviewService.getReview(1)).willReturn(new InvestmentNewsReviewResponse(LocalDate.of(2026, 9, 18),
			List.of(new Review(3, "밀 가격 상승", "밀 생산량이 줄었어요", true, "고소한빵", 1000, 900, "재료비 증가"))));
		mockMvc.perform(get(NEWS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reviews[0].ready").value(true))
			.andExpect(jsonPath("$.reviews[0].currentPrice").value(900));
	}

	@Test
	void getReview_unauthorized() throws Exception {
		mockMvc.perform(get(NEWS_URL)).andExpect(status().isUnauthorized());
	}

	@Test
	void getReview_noFamily() throws Exception {
		givenAuthenticated();
		given(reviewService.getReview(1)).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		mockMvc.perform(get(NEWS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound());
	}
}
