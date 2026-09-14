package com.bana.dotori.investment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.dto.InvestmentProductSummary;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.service.InvestmentProductService;

@WebMvcTest(InvestmentProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class InvestmentProductControllerTest {

	private static final String PRODUCTS_URL = "/api/v1/investments/products";
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
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	private InvestmentProductSummary summary() {
		return new InvestmentProductSummary(1, "시원아이스", InvestmentProductCategory.STOCK, 1200, 4.35, true);
	}

	@Test
	@DisplayName("투자 상품 목록 조회 성공 시 200과 명세 형식의 목록을 반환한다")
	void getProducts_success() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getProducts(any(), any()))
			.willReturn(new InvestmentProductListResponse(List.of(summary())));

		mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.products[0].investmentProductId").value(1))
			.andExpect(jsonPath("$.products[0].name").value("시원아이스"))
			.andExpect(jsonPath("$.products[0].category").value("STOCK"))
			.andExpect(jsonPath("$.products[0].currentPrice").value(1200))
			.andExpect(jsonPath("$.products[0].changeRate").value(4.35))
			.andExpect(jsonPath("$.products[0].isActive").value(true));
	}

	@Test
	@DisplayName("인증 사용자 ID와 category 파라미터를 서비스에 그대로 전달한다")
	void getProducts_withCategory() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getProducts(any(), any()))
			.willReturn(new InvestmentProductListResponse(List.of()));

		mockMvc.perform(get(PRODUCTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("category", "STOCK"))
			.andExpect(status().isOk());

		then(investmentProductService).should().getProducts(eq(1), eq(InvestmentProductCategory.STOCK));
	}

	@Test
	@DisplayName("category 값이 올바르지 않으면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void getProducts_invalidCategory() throws Exception {
		givenAuthenticated();

		mockMvc.perform(get(PRODUCTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("category", "음식"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(investmentProductService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("소속 가족이 없으면 404와 FAMILY_NOT_FOUND 에러를 반환한다")
	void getProducts_noFamily() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getProducts(any(), any()))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("토큰 없이 호출하면 401과 UNAUTHORIZED 에러를 반환한다")
	void getProducts_unauthorized() throws Exception {
		mockMvc.perform(get(PRODUCTS_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(investmentProductService).shouldHaveNoInteractions();
	}
}
