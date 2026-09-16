package com.bana.dotori.investment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.bana.dotori.investment.dto.InvestmentHoldingSummary;
import com.bana.dotori.investment.dto.InvestmentHoldingsResponse;
import com.bana.dotori.investment.service.InvestmentProductService;

@WebMvcTest(InvestmentHoldingsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class InvestmentHoldingsControllerTest {

	private static final String HOLDINGS_URL = "/api/v1/investments/holdings";
	private static final String BEARER = "Bearer valid-access-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InvestmentProductService investmentProductService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication("valid-access-token"))
			.willReturn(new UsernamePasswordAuthenticationToken(1, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
	}

	@Test
	@DisplayName("보유 투자 상품 조회 성공 시 200과 명세 형식의 목록을 반환한다")
	void getHoldings_success() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getHoldings(any(), any()))
			.willReturn(new InvestmentHoldingsResponse(List.of(
				new InvestmentHoldingSummary(1, "시원아이스", 15, 1100, 1300))));

		mockMvc.perform(get(HOLDINGS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.holdings[0].investmentProductId").value(1))
			.andExpect(jsonPath("$.holdings[0].productName").value("시원아이스"))
			.andExpect(jsonPath("$.holdings[0].quantity").value(15))
			.andExpect(jsonPath("$.holdings[0].averageBuyPrice").value(1100))
			.andExpect(jsonPath("$.holdings[0].currentPrice").value(1300));
	}

	@Test
	@DisplayName("childId 파라미터를 서비스에 그대로 전달하고, 없으면 null 로 전달한다")
	void getHoldings_childIdParam() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getHoldings(any(), any()))
			.willReturn(new InvestmentHoldingsResponse(List.of()));

		mockMvc.perform(get(HOLDINGS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("childId", "2"))
			.andExpect(status().isOk());
		then(investmentProductService).should().getHoldings(eq(1), eq(2));

		mockMvc.perform(get(HOLDINGS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());
		then(investmentProductService).should().getHoldings(eq(1), isNull());
	}

	@Test
	@DisplayName("내 가족의 자녀가 아니면 403과 NOT_FAMILY_CHILD 에러를 반환한다")
	void getHoldings_notFamilyChild() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getHoldings(any(), any()))
			.willThrow(new BusinessException(ErrorCode.NOT_FAMILY_CHILD));

		mockMvc.perform(get(HOLDINGS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("childId", "99"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_FAMILY_CHILD"));
	}

	@Test
	@DisplayName("토큰 없이 호출하면 401과 UNAUTHORIZED 에러를 반환한다")
	void getHoldings_unauthorized() throws Exception {
		mockMvc.perform(get(HOLDINGS_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(investmentProductService).shouldHaveNoInteractions();
	}
}
