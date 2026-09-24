package com.bana.dotori.investment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.bana.dotori.investment.dto.InvestmentTransactionListResponse;
import com.bana.dotori.investment.dto.InvestmentTransactionSummary;
import com.bana.dotori.investment.entity.InvestmentTransactionType;
import com.bana.dotori.investment.service.InvestmentProductService;

@WebMvcTest(InvestmentTransactionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class InvestmentTransactionControllerTest {

	private static final String TRANSACTIONS_URL = "/api/v1/investments/transactions";
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
	@DisplayName("거래 이력 조회 성공 시 200과 명세 형식의 목록을 반환한다")
	void getTransactions_success() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getTransactions(any(), any(), any(), any(), any()))
			.willReturn(new InvestmentTransactionListResponse(List.of(new InvestmentTransactionSummary(
				1, "시원아이스", InvestmentTransactionType.BUY, 1, 1000, 1000,
				OffsetDateTime.parse("2026-09-10T10:00:00+09:00")))));

		mockMvc.perform(get(TRANSACTIONS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.transactions[0].investmentTransactionId").value(1))
			.andExpect(jsonPath("$.transactions[0].productName").value("시원아이스"))
			.andExpect(jsonPath("$.transactions[0].transactionType").value("BUY"))
			.andExpect(jsonPath("$.transactions[0].quantity").value(1))
			.andExpect(jsonPath("$.transactions[0].unitPrice").value(1000))
			.andExpect(jsonPath("$.transactions[0].totalAmount").value(1000))
			.andExpect(jsonPath("$.transactions[0].createdAt").exists());
	}

	@Test
	@DisplayName("childId·기간·type 파라미터를 서비스에 그대로 전달한다")
	void getTransactions_withFilters() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getTransactions(any(), any(), any(), any(), any()))
			.willReturn(new InvestmentTransactionListResponse(List.of()));

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("childId", "2")
				.param("startDate", "2026-09-01")
				.param("endDate", "2026-09-15")
				.param("type", "BUY"))
			.andExpect(status().isOk());

		then(investmentProductService).should().getTransactions(eq(1), eq(2),
			eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 15)),
			eq(InvestmentTransactionType.BUY));
	}

	@Test
	@DisplayName("파라미터 없이 호출하면 전부 null 로 전달한다")
	void getTransactions_noFilters() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getTransactions(any(), any(), any(), any(), any()))
			.willReturn(new InvestmentTransactionListResponse(List.of()));

		mockMvc.perform(get(TRANSACTIONS_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk());

		then(investmentProductService).should().getTransactions(eq(1), isNull(), isNull(), isNull(), isNull());
	}

	@Test
	@DisplayName("type 값이 올바르지 않으면 400과 INVALID_REQUEST 에러를 반환한다")
	void getTransactions_invalidType() throws Exception {
		givenAuthenticated();

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("type", "HOLD"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(investmentProductService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("내 가족의 자녀가 아니면 403과 NOT_FAMILY_CHILD 에러를 반환한다")
	void getTransactions_notFamilyChild() throws Exception {
		givenAuthenticated();
		given(investmentProductService.getTransactions(any(), any(), any(), any(), any()))
			.willThrow(new BusinessException(ErrorCode.NOT_FAMILY_CHILD));

		mockMvc.perform(get(TRANSACTIONS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.param("childId", "99"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_FAMILY_CHILD"));
	}

	@Test
	@DisplayName("토큰 없이 호출하면 401과 UNAUTHORIZED 에러를 반환한다")
	void getTransactions_unauthorized() throws Exception {
		mockMvc.perform(get(TRANSACTIONS_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(investmentProductService).shouldHaveNoInteractions();
	}
}
