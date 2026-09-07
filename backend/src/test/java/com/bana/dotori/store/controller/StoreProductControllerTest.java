package com.bana.dotori.store.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.bana.dotori.store.dto.StoreProductDetailResponse;
import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.dto.StoreProductSummary;
import com.bana.dotori.store.entity.StoreProductCategory;
import com.bana.dotori.store.service.StoreProductService;

@WebMvcTest(StoreProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class StoreProductControllerTest {

	private static final String PRODUCTS_URL = "/api/v1/families/10/store/products";
	private static final String TOKEN = "access-token";
	private static final String BEARER = "Bearer " + TOKEN;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StoreProductService storeProductService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private void givenAuthenticated(Integer userId) {
		given(jwtTokenProvider.getAuthentication(TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
	}

	@Test
	@DisplayName("상품 목록 조회 성공 시 200과 상품 목록을 반환한다")
	void getProducts_success() throws Exception {
		givenAuthenticated(1);
		OffsetDateTime createdAt = OffsetDateTime.of(2026, 8, 28, 9, 0, 0, 0, ZoneOffset.ofHours(9));
		given(storeProductService.getProducts(1, 10)).willReturn(new StoreProductListResponse(List.of(
			new StoreProductSummary(1, "게임 30분 이용권", StoreProductCategory.GAME, 3000, 5, createdAt),
			new StoreProductSummary(2, "자전거", StoreProductCategory.TOY, null, 1, createdAt))));

		mockMvc.perform(get(PRODUCTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.products.length()").value(2))
			.andExpect(jsonPath("$.products[0].storeProductId").value(1))
			.andExpect(jsonPath("$.products[0].name").value("게임 30분 이용권"))
			.andExpect(jsonPath("$.products[0].category").value("GAME"))
			.andExpect(jsonPath("$.products[0].price").value(3000))
			.andExpect(jsonPath("$.products[0].quantity").value(5))
			.andExpect(jsonPath("$.products[0].createdAt").exists())
			.andExpect(jsonPath("$.products[1].price").doesNotExist());
	}

	@Test
	@DisplayName("토큰 없이 요청하면 401을 반환한다")
	void getProducts_unauthorized() throws Exception {
		mockMvc.perform(get(PRODUCTS_URL))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("소속되지 않은 가족이면 404와 FAMILY_NOT_FOUND 에러를 반환한다")
	void getProducts_familyNotFound() throws Exception {
		givenAuthenticated(1);
		given(storeProductService.getProducts(1, 10))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(get(PRODUCTS_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("상품 상세 조회 성공 시 200과 상세 정보를 반환한다")
	void getProduct_success() throws Exception {
		givenAuthenticated(1);
		OffsetDateTime createdAt = OffsetDateTime.of(2026, 8, 28, 9, 0, 0, 0, ZoneOffset.ofHours(9));
		given(storeProductService.getProduct(1, 10, 5)).willReturn(new StoreProductDetailResponse(
			5, "자전거", "주말에만 사용 가능", StoreProductCategory.TOY, new BigDecimal("90.00"),
			900000, 1, 1, createdAt));

		mockMvc.perform(get(PRODUCTS_URL + "/5")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.storeProductId").value(5))
			.andExpect(jsonPath("$.name").value("자전거"))
			.andExpect(jsonPath("$.description").value("주말에만 사용 가능"))
			.andExpect(jsonPath("$.category").value("TOY"))
			.andExpect(jsonPath("$.value").value(90.00))
			.andExpect(jsonPath("$.price").value(900000))
			.andExpect(jsonPath("$.quantity").value(1))
			.andExpect(jsonPath("$.createdBy").value(1))
			.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	@DisplayName("토큰 없이 상세 조회하면 401을 반환한다")
	void getProduct_unauthorized() throws Exception {
		mockMvc.perform(get(PRODUCTS_URL + "/5"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("상품이 없으면 404와 PRODUCT_NOT_FOUND 에러를 반환한다")
	void getProduct_notFound() throws Exception {
		givenAuthenticated(1);
		given(storeProductService.getProduct(1, 10, 999))
			.willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

		mockMvc.perform(get(PRODUCTS_URL + "/999")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}
}
