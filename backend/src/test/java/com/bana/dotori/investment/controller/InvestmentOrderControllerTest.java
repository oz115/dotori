package com.bana.dotori.investment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import com.bana.dotori.investment.dto.InvestmentOrderRequest;
import com.bana.dotori.investment.dto.InvestmentOrderResponse;
import com.bana.dotori.investment.entity.InvestmentTransactionType;
import com.bana.dotori.investment.service.InvestmentOrderService;

@WebMvcTest(InvestmentOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class InvestmentOrderControllerTest {

	private static final String BUY_URL = "/api/v1/investments/orders/buy";
	private static final String SELL_URL = "/api/v1/investments/orders/sell";
	private static final String BEARER = "Bearer valid-access-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InvestmentOrderService investmentOrderService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated() {
		given(jwtTokenProvider.getAuthentication("valid-access-token"))
			.willReturn(new UsernamePasswordAuthenticationToken(1, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	private InvestmentOrderResponse buyResponse() {
		return new InvestmentOrderResponse(100, InvestmentTransactionType.BUY, 1, 1200, 1200, 8000,
			OffsetDateTime.parse("2026-09-16T12:00:00+09:00"));
	}

	@Test
	@DisplayName("매수 성공 시 201과 체결 결과를 반환한다")
	void buy_success() throws Exception {
		givenAuthenticated();
		given(investmentOrderService.buy(eq(1), any(InvestmentOrderRequest.class))).willReturn(buyResponse());

		mockMvc.perform(post(BUY_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"investmentProductId\": 1}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.investmentTransactionId").value(100))
			.andExpect(jsonPath("$.transactionType").value("BUY"))
			.andExpect(jsonPath("$.quantity").value(1))
			.andExpect(jsonPath("$.unitPrice").value(1200))
			.andExpect(jsonPath("$.totalAmount").value(1200))
			.andExpect(jsonPath("$.pointBalanceAfter").value(8000))
			.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	@DisplayName("종목 ID 없이 매수하면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void buy_missingProductId() throws Exception {
		givenAuthenticated();

		mockMvc.perform(post(BUY_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(investmentOrderService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("포인트가 부족하면 400과 INSUFFICIENT_POINT 에러를 반환한다")
	void buy_insufficientPoint() throws Exception {
		givenAuthenticated();
		given(investmentOrderService.buy(eq(1), any(InvestmentOrderRequest.class)))
			.willThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT));

		mockMvc.perform(post(BUY_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"investmentProductId\": 1}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_POINT"));
	}

	@Test
	@DisplayName("거래 중지 종목이면 409와 PRODUCT_INACTIVE 에러를 반환한다")
	void buy_inactiveProduct() throws Exception {
		givenAuthenticated();
		given(investmentOrderService.buy(eq(1), any(InvestmentOrderRequest.class)))
			.willThrow(new BusinessException(ErrorCode.PRODUCT_INACTIVE));

		mockMvc.perform(post(BUY_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"investmentProductId\": 1}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PRODUCT_INACTIVE"));
	}

	@Test
	@DisplayName("매도 성공 시 201과 체결 결과를 반환한다")
	void sell_success() throws Exception {
		givenAuthenticated();
		given(investmentOrderService.sell(eq(1), any(InvestmentOrderRequest.class)))
			.willReturn(new InvestmentOrderResponse(101, InvestmentTransactionType.SELL, 1, 1300, 1300, 9300,
				OffsetDateTime.parse("2026-09-16T13:00:00+09:00")));

		mockMvc.perform(post(SELL_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"investmentProductId\": 1}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.investmentTransactionId").value(101))
			.andExpect(jsonPath("$.transactionType").value("SELL"))
			.andExpect(jsonPath("$.quantity").value(1))
			.andExpect(jsonPath("$.unitPrice").value(1300))
			.andExpect(jsonPath("$.pointBalanceAfter").value(9300));
	}

	@Test
	@DisplayName("보유하지 않은 종목을 매도하면 400과 INSUFFICIENT_QUANTITY 에러를 반환한다")
	void sell_notHolding() throws Exception {
		givenAuthenticated();
		given(investmentOrderService.sell(eq(1), any(InvestmentOrderRequest.class)))
			.willThrow(new BusinessException(ErrorCode.INSUFFICIENT_QUANTITY));

		mockMvc.perform(post(SELL_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"investmentProductId\": 1}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_QUANTITY"));
	}

	@Test
	@DisplayName("토큰 없이 매수하면 401과 UNAUTHORIZED 에러를 반환한다")
	void buy_unauthorized() throws Exception {
		mockMvc.perform(post(BUY_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"investmentProductId\": 1}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(investmentOrderService).shouldHaveNoInteractions();
	}
}
