package com.bana.dotori.savings.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.savings.dto.response.SavingsExpectedReturnResponse;
import com.bana.dotori.savings.dto.response.SavingsProductResponse;
import com.bana.dotori.savings.dto.response.SavingsSubscriptionResponse;
import com.bana.dotori.savings.dto.response.SavingsSummaryResponse;
import com.bana.dotori.savings.dto.response.SavingsTransactionResponse;
import com.bana.dotori.savings.entity.ContributionStyle;
import com.bana.dotori.savings.entity.PayoutTiming;
import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.entity.SavingsTransactionType;
import com.bana.dotori.savings.service.SavingsService;

/**
 * 컨트롤러 계층 - HTTP 경계와 본인 확인만 검증한다. 서비스는 가짜라 실제 지갑 차감은 없다.
 */
@WebMvcTest(SavingsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
@DisplayName("예적금 API")
class SavingsControllerTest {

	private static final String BASE_URL = "/api/v1/families/{familyId}/children/{childId}/savings";
	private static final Integer FAMILY_ID = 1;
	private static final Integer CHILD_ID = 2;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SavingsService savingsService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private SavingsProductResponse productResponse() {
		return new SavingsProductResponse(1, "보통예금", SavingsProductType.ORDINARY, "설명", 1,
			BigDecimal.valueOf(0.1), ContributionStyle.FREE, PayoutTiming.WEEKLY_IMMEDIATE, null, null,
			"교육목적", "안내문구", 40_000L);
	}

	private SavingsSubscriptionResponse subscriptionResponse() {
		return subscriptionResponse(SavingsSubscriptionStatus.ACTIVE);
	}

	private SavingsSubscriptionResponse subscriptionResponse(SavingsSubscriptionStatus status) {
		return new SavingsSubscriptionResponse(1, 1, "보통예금", SavingsProductType.ORDINARY,
			ContributionStyle.FREE, null, 10_000, 10_000, 0, 10_000, 0, status, OffsetDateTime.now(), null,
			OffsetDateTime.now().plusWeeks(1), null);
	}

	@Test
	@DisplayName("상품 목록을 조회하면 200과 목록을 돌려준다")
	void 상품_목록_조회() throws Exception {
		given(savingsService.listProducts(CHILD_ID, FAMILY_ID, CHILD_ID)).willReturn(List.of(productResponse()));

		mockMvc.perform(get(BASE_URL + "/products", FAMILY_ID, CHILD_ID).with(loginAs(CHILD_ID, "CHILD")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("보통예금"))
			.andExpect(jsonPath("$[0].maxAmount").value(40_000));
	}

	@Test
	@DisplayName("상품 상세를 조회하면 200과 상품 하나를 돌려준다")
	void 상품_상세_조회() throws Exception {
		given(savingsService.getProduct(CHILD_ID, FAMILY_ID, CHILD_ID, 1)).willReturn(productResponse());

		mockMvc.perform(get(BASE_URL + "/products/{productId}", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(CHILD_ID, "CHILD")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.savingsProductId").value(1))
			.andExpect(jsonPath("$.name").value("보통예금"));
	}

	@Test
	@DisplayName("예상 손익을 조회하면 200과 만기·해지 금액을 돌려준다")
	void 예상_손익_조회() throws Exception {
		given(savingsService.getExpectedReturn(CHILD_ID, FAMILY_ID, CHILD_ID, 1))
			.willReturn(new SavingsExpectedReturnResponse(1, 10_000, 375, 1_500L, 11_500L, 10_000L,
				OffsetDateTime.now().plusWeeks(3)));

		mockMvc.perform(get(BASE_URL + "/subscriptions/{id}/expected-return", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(CHILD_ID, "CHILD")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.expectedInterest").value(1_500))
			.andExpect(jsonPath("$.expectedTotal").value(11_500))
			.andExpect(jsonPath("$.earlyTerminationAmount").value(10_000));
	}

	@Test
	@DisplayName("부모도 아이의 예상 손익을 조회할 수 있다")
	void 부모도_예상_손익을_본다() throws Exception {
		Integer parentId = 9;
		given(savingsService.getExpectedReturn(parentId, FAMILY_ID, CHILD_ID, 1))
			.willReturn(new SavingsExpectedReturnResponse(1, 10_000, 0, 1_500L, 11_500L, 10_000L, null));

		mockMvc.perform(get(BASE_URL + "/subscriptions/{id}/expected-return", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(parentId, "PARENT")))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("본인이 가입하면 201과 가입 결과를 돌려준다")
	void 본인_가입_성공() throws Exception {
		given(savingsService.subscribe(eq(CHILD_ID), eq(FAMILY_ID), eq(CHILD_ID), any(), eq("idem-1")))
			.willReturn(subscriptionResponse());

		mockMvc.perform(post(BASE_URL + "/subscriptions", FAMILY_ID, CHILD_ID).with(loginAs(CHILD_ID, "CHILD"))
				.header("Idempotency-Key", "idem-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"savingsProductId\": 1, \"amount\": 10000}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.principal").value(10_000))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	@DisplayName("본인이 아니면 가입을 거부하고 서비스를 호출하지 않는다")
	void 본인이_아니면_거부한다() throws Exception {
		Integer sibling = 3;

		mockMvc.perform(post(BASE_URL + "/subscriptions", FAMILY_ID, CHILD_ID).with(loginAs(sibling, "CHILD"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"savingsProductId\": 1, \"amount\": 10000}"))
			.andExpect(status().isForbidden());

		verify(savingsService, never()).subscribe(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("가입 금액이 없으면 400을 돌려준다")
	void 금액_누락시_400() throws Exception {
		mockMvc.perform(post(BASE_URL + "/subscriptions", FAMILY_ID, CHILD_ID).with(loginAs(CHILD_ID, "CHILD"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"savingsProductId\": 1}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("본인이 납입하면 201과 갱신된 가입 상태를 돌려준다")
	void 본인_납입_성공() throws Exception {
		given(savingsService.contribute(eq(CHILD_ID), eq(FAMILY_ID), eq(CHILD_ID), eq(1), any(), eq("idem-2")))
			.willReturn(subscriptionResponse());

		mockMvc.perform(post(BASE_URL + "/subscriptions/{id}/contributions", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(CHILD_ID, "CHILD"))
				.header("Idempotency-Key", "idem-2")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 1000}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.currentAmount").value(10_000));
	}

	@Test
	@DisplayName("납입 금액이 없으면 400을 돌려준다")
	void 납입금액_누락시_400() throws Exception {
		mockMvc.perform(post(BASE_URL + "/subscriptions/{id}/contributions", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(CHILD_ID, "CHILD"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("부모가 아이 대신 납입하려 하면 거부한다")
	void 부모는_대신_납입할_수_없다() throws Exception {
		Integer parentId = 9;

		mockMvc.perform(post(BASE_URL + "/subscriptions/{id}/contributions", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(parentId, "PARENT"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 1000}"))
			.andExpect(status().isForbidden());

		verify(savingsService, never()).contribute(any(), any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("본인이 만기 수령하면 200과 수령 완료 상태를 돌려준다")
	void 본인_수령_성공() throws Exception {
		given(savingsService.receive(CHILD_ID, FAMILY_ID, CHILD_ID, 1, "idem-3"))
			.willReturn(subscriptionResponse(SavingsSubscriptionStatus.COMPLETED));

		mockMvc.perform(post(BASE_URL + "/subscriptions/{id}/receipt", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(CHILD_ID, "CHILD"))
				.header("Idempotency-Key", "idem-3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	@DisplayName("본인이 중도 해지하면 200과 해지 상태를 돌려준다")
	void 본인_해지_성공() throws Exception {
		given(savingsService.terminate(CHILD_ID, FAMILY_ID, CHILD_ID, 1, null))
			.willReturn(subscriptionResponse(SavingsSubscriptionStatus.CANCELLED));

		mockMvc.perform(post(BASE_URL + "/subscriptions/{id}/termination", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(CHILD_ID, "CHILD")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	@DisplayName("부모가 아이 예적금을 해지하려 하면 거부한다")
	void 부모는_대신_해지할_수_없다() throws Exception {
		Integer parentId = 9;

		mockMvc.perform(post(BASE_URL + "/subscriptions/{id}/termination", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(parentId, "PARENT")))
			.andExpect(status().isForbidden());

		verify(savingsService, never()).terminate(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("가입 내역 목록을 조회하면 200을 돌려준다")
	void 가입내역_목록_조회() throws Exception {
		given(savingsService.listSubscriptions(CHILD_ID, FAMILY_ID, CHILD_ID))
			.willReturn(List.of(subscriptionResponse()));

		mockMvc.perform(get(BASE_URL + "/subscriptions", FAMILY_ID, CHILD_ID).with(loginAs(CHILD_ID, "CHILD")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].savingsSubscriptionId").value(1));
	}

	@Test
	@DisplayName("가입 내역 상세를 조회하면 200을 돌려준다")
	void 가입내역_상세_조회() throws Exception {
		given(savingsService.getSubscription(CHILD_ID, FAMILY_ID, CHILD_ID, 1)).willReturn(subscriptionResponse());

		mockMvc.perform(get(BASE_URL + "/subscriptions/{id}", FAMILY_ID, CHILD_ID, 1)
				.with(loginAs(CHILD_ID, "CHILD")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.productName").value("보통예금"));
	}

	@Test
	@DisplayName("거래 이력을 조회하면 200과 최신순 목록을 돌려준다")
	void 거래이력_조회() throws Exception {
		given(savingsService.listTransactions(CHILD_ID, FAMILY_ID, CHILD_ID)).willReturn(List.of(
			new SavingsTransactionResponse(5, 1, 1, "단리예금", SavingsProductType.SIMPLE_INTEREST,
				SavingsTransactionType.INITIAL_DEPOSIT, 3_000, OffsetDateTime.now())));

		mockMvc.perform(get(BASE_URL + "/transactions", FAMILY_ID, CHILD_ID).with(loginAs(CHILD_ID, "CHILD")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].productName").value("단리예금"))
			.andExpect(jsonPath("$[0].transactionType").value("INITIAL_DEPOSIT"))
			.andExpect(jsonPath("$[0].amount").value(3_000));
	}

	@Test
	@DisplayName("부모도 아이의 거래 이력을 조회할 수 있다")
	void 부모도_거래이력을_본다() throws Exception {
		Integer parentId = 9;
		given(savingsService.listTransactions(parentId, FAMILY_ID, CHILD_ID)).willReturn(List.of());

		mockMvc.perform(get(BASE_URL + "/transactions", FAMILY_ID, CHILD_ID).with(loginAs(parentId, "PARENT")))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("부모가 요약을 조회하면 200과 집계값을 돌려준다")
	void 부모_요약_조회() throws Exception {
		Integer parentId = 9;
		given(savingsService.getSummary(parentId, FAMILY_ID, CHILD_ID))
			.willReturn(new SavingsSummaryResponse(2, 1, 30_000, 1_500, 31_500, OffsetDateTime.now().plusWeeks(2)));

		mockMvc.perform(get(BASE_URL + "/summary", FAMILY_ID, CHILD_ID).with(loginAs(parentId, "PARENT")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activeCount").value(2))
			.andExpect(jsonPath("$.maturedCount").value(1))
			.andExpect(jsonPath("$.totalAmount").value(31_500));
	}

	@Test
	@DisplayName("아이 본인도 요약을 조회할 수 있다")
	void 본인_요약_조회() throws Exception {
		given(savingsService.getSummary(CHILD_ID, FAMILY_ID, CHILD_ID))
			.willReturn(new SavingsSummaryResponse(0, 0, 0, 0, 0, null));

		mockMvc.perform(get(BASE_URL + "/summary", FAMILY_ID, CHILD_ID).with(loginAs(CHILD_ID, "CHILD")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activeCount").value(0))
			.andExpect(jsonPath("$.nearestMaturityAt").doesNotExist());
	}

	private static RequestPostProcessor loginAs(Integer userId, String role) {
		return authentication(new UsernamePasswordAuthenticationToken(userId, null,
			List.of(new SimpleGrantedAuthority("ROLE_" + role))));
	}
}
