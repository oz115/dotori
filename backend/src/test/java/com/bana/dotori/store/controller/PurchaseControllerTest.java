package com.bana.dotori.store.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import com.bana.dotori.store.dto.PurchaseHistoryItem;
import com.bana.dotori.store.dto.PurchaseHistoryResponse;
import com.bana.dotori.store.entity.StoreProductCategory;
import com.bana.dotori.store.service.PurchaseHistoryService;

@WebMvcTest(PurchaseController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class PurchaseControllerTest {

	private static final String PURCHASES_URL = "/api/v1/families/10/children/1/store/purchases";
	private static final String TOKEN = "access-token";
	private static final String BEARER = "Bearer " + TOKEN;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PurchaseHistoryService purchaseHistoryService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated(Integer userId) {
		given(jwtTokenProvider.getAuthentication(TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
	}

	@Test
	@DisplayName("구매 내역 조회 성공 시 200과 목록을 반환한다")
	void getPurchases_success() throws Exception {
		givenAuthenticated(1);
		OffsetDateTime purchasedAt = OffsetDateTime.of(2026, 8, 28, 14, 0, 0, 0, ZoneOffset.ofHours(9));
		given(purchaseHistoryService.getPurchases(eq(10), eq(1), eq(1), isNull(), isNull()))
			.willReturn(new PurchaseHistoryResponse(
				List.of(new PurchaseHistoryItem(1, 5, "게임 30분 이용권", StoreProductCategory.GAME, 1, 3000,
					purchasedAt))));

		mockMvc.perform(get(PURCHASES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.purchases.length()").value(1))
			.andExpect(jsonPath("$.purchases[0].purchaseId").value(1))
			.andExpect(jsonPath("$.purchases[0].storeProductId").value(5))
			.andExpect(jsonPath("$.purchases[0].productName").value("게임 30분 이용권"))
			.andExpect(jsonPath("$.purchases[0].category").value("GAME"))
			.andExpect(jsonPath("$.purchases[0].quantity").value(1))
			.andExpect(jsonPath("$.purchases[0].totalPrice").value(3000))
			.andExpect(jsonPath("$.purchases[0].purchasedAt").exists());
	}

	@Test
	@DisplayName("기간 조건이 있으면 그대로 서비스에 전달한다")
	void getPurchases_withDateRange() throws Exception {
		givenAuthenticated(1);
		given(purchaseHistoryService.getPurchases(eq(10), eq(1), eq(1),
			eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 8, 28))))
			.willReturn(new PurchaseHistoryResponse(List.of()));

		mockMvc.perform(get(PURCHASES_URL + "?startDate=2026-08-01&endDate=2026-08-28")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.purchases.length()").value(0));
	}

	@Test
	@DisplayName("빈 구매 내역이면 빈 배열을 반환한다")
	void getPurchases_empty() throws Exception {
		givenAuthenticated(1);
		given(purchaseHistoryService.getPurchases(eq(10), eq(1), eq(1), isNull(), isNull()))
			.willReturn(new PurchaseHistoryResponse(List.of()));

		mockMvc.perform(get(PURCHASES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.purchases.length()").value(0));
	}

	@Test
	@DisplayName("토큰 없이 요청하면 401을 반환한다")
	void getPurchases_unauthorized() throws Exception {
		mockMvc.perform(get(PURCHASES_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("소속되지 않은 가족이면 404와 FAMILY_NOT_FOUND 에러를 반환한다")
	void getPurchases_familyNotFound() throws Exception {
		givenAuthenticated(1);
		given(purchaseHistoryService.getPurchases(eq(10), eq(1), eq(1), isNull(), isNull()))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(get(PURCHASES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("남의 구매 내역을 조회하면 403과 FORBIDDEN 에러를 반환한다")
	void getPurchases_forbidden() throws Exception {
		givenAuthenticated(1);
		given(purchaseHistoryService.getPurchases(eq(10), eq(1), eq(1), isNull(), isNull()))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(get(PURCHASES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("조회 시작일이 종료일보다 늦으면 400과 INVALID_DATE_RANGE 에러를 반환한다")
	void getPurchases_invalidDateRange() throws Exception {
		givenAuthenticated(1);
		given(purchaseHistoryService.getPurchases(eq(10), eq(1), eq(1),
			eq(LocalDate.of(2026, 8, 28)), eq(LocalDate.of(2026, 8, 1))))
			.willThrow(new BusinessException(ErrorCode.INVALID_DATE_RANGE));

		mockMvc.perform(get(PURCHASES_URL + "?startDate=2026-08-28&endDate=2026-08-01")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
	}
}
