package com.bana.dotori.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointChargeService;
import com.bana.dotori.wallet.service.PointGrantService;

/**
 * 컨트롤러 계층 - HTTP 경계만 검증한다.
 *
 * <p>서비스를 가짜로 대체했으므로 잔액 계산이나 송금은 전혀 일어나지 않는다.
 * 여기가 실패하면 원인은 요청 매핑, 검증 애노테이션, 예외 → 상태코드 매핑 중 하나다.
 *
 * <p>팀 규칙대로 SecurityConfig 를 함께 올려 시큐리티 규칙까지 슬라이스에 포함한다.
 * 다만 필터는 꺼 둔다. 로그인·토큰 발급(AUTH-02)이 없어 인증된 요청을 만들 수 없고,
 * 필터를 켜면 모든 요청이 인증 단계에서 막혀 정작 보려는 매핑 검증에 도달하지 못한다.
 * 보호 여부 자체는 {@link PointWalletApiSecurityTest} 가 필터를 켠 채로 확인한다.
 */
@WebMvcTest(PointWalletController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})

@DisplayName("포인트 충전 API")
class PointWalletControllerTest {

	private static final String CHARGE_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/charge";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PointChargeService pointChargeService;

	@MockitoBean
	private PointGrantService pointGrantService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("충전에 성공하면 201 과 명세 형식의 응답을 돌려준다")
	void 충전_성공() throws Exception {
		given(pointChargeService.charge(eq(1), eq(2), eq(10_000), eq("charge-001")))
				.willReturn(result(100, 10_000, 15_000));

		mockMvc.perform(post(CHARGE_URL, 1, 2).with(loginAs(2, "CHILD"))
						.header("Idempotency-Key", "charge-001")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pointTransactionId").value(100))
				.andExpect(jsonPath("$.transactionType").value("CHARGE"))
				.andExpect(jsonPath("$.amount").value(10_000))
				.andExpect(jsonPath("$.balanceAfter").value(15_000))
				.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	@DisplayName("멱등키 헤더가 없어도 요청은 처리된다")
	void 멱등키_없이도_처리된다() throws Exception {
		given(pointChargeService.charge(eq(1), eq(2), eq(10_000), isNull()))
				.willReturn(result(101, 10_000, 10_000));

		mockMvc.perform(post(CHARGE_URL, 1, 2).with(loginAs(2, "CHILD"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("금액이 0 이하면 서비스를 호출하지 않고 400 INVALID_REQUEST 로 막는다")
	void 잘못된_금액은_요청_검증에서_막힌다() throws Exception {
		mockMvc.perform(post(CHARGE_URL, 1, 2).with(loginAs(2, "CHILD"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verify(pointChargeService, never()).charge(any(), any(), any(Integer.class), any());
	}

	@Test
	@DisplayName("금액이 없으면 400 으로 막는다")
	void 금액_누락은_400이다() throws Exception {
		mockMvc.perform(post(CHARGE_URL, 1, 2).with(loginAs(2, "CHILD"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("계좌 잔액이 부족하면 400 INSUFFICIENT_ACCOUNT_BALANCE 로 매핑한다")
	void 계좌_잔액_부족은_400이다() throws Exception {
		willThrow(new BusinessException(ErrorCode.INSUFFICIENT_ACCOUNT_BALANCE, "계좌 잔액 부족"))
				.given(pointChargeService).charge(any(), any(), any(Integer.class), any());

		mockMvc.perform(post(CHARGE_URL, 1, 2).with(loginAs(2, "CHILD"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_ACCOUNT_BALANCE"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	@DisplayName("연동 계좌가 없으면 404 ACCOUNT_NOT_LINKED 로 매핑한다")
	void 계좌_미연동은_404다() throws Exception {
		willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED, "연동 계좌 없음"))
				.given(pointChargeService).charge(any(), any(), any(Integer.class), any());

		mockMvc.perform(post(CHARGE_URL, 1, 2).with(loginAs(2, "CHILD"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_LINKED"));
	}

	@Test
	@DisplayName("지갑이 없으면 404 WALLET_NOT_FOUND 로 매핑한다")
	void 지갑_없음은_404다() throws Exception {
		willThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND, "지갑 없음"))
				.given(pointChargeService).charge(any(), any(), any(Integer.class), any());

		mockMvc.perform(post(CHARGE_URL, 1, 2).with(loginAs(2, "CHILD"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));
	}

	@Test
	@DisplayName("서비스가 던진 도메인 예외도 각자의 코드로 매핑된다")
	void 도메인_예외가_코드로_매핑된다() throws Exception {
		willThrow(new BusinessException(ErrorCode.INVALID_POINT_AMOUNT, "금액 오류"))
				.given(pointChargeService).charge(any(), any(), any(Integer.class), any());

		mockMvc.perform(post(CHARGE_URL, 1, 2).with(loginAs(2, "CHILD"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_POINT_AMOUNT"));
	}

	@Nested
	@DisplayName("포인트 지급")
	class Grant {

		private static final String GRANT_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/grant";

		@Test
		@DisplayName("지급에 성공하면 201 과 GRANT 응답을 돌려준다")
		void 지급_성공() throws Exception {
			given(pointGrantService.grant(any(), eq(1), eq(2), eq(5_000), eq("용돈"), eq("grant-001")))
					.willReturn(grantResult(101, 5_000, 20_000));

			mockMvc.perform(post(GRANT_URL, 1, 2).with(loginAs(9, "PARENT"))
							.header("Idempotency-Key", "grant-001")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"childId\": 2, \"amount\": 5000, \"description\": \"용돈\"}"))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.pointTransactionId").value(101))
					.andExpect(jsonPath("$.transactionType").value("GRANT"))
					.andExpect(jsonPath("$.amount").value(5_000))
					.andExpect(jsonPath("$.balanceAfter").value(20_000));
		}

		@Test
		@DisplayName("본문에 childId 가 없어도 경로 값으로 처리한다")
		void 본문_childId가_없어도_된다() throws Exception {
			given(pointGrantService.grant(any(), eq(1), eq(2), eq(5_000), any(), any()))
					.willReturn(grantResult(101, 5_000, 20_000));

			mockMvc.perform(post(GRANT_URL, 1, 2).with(loginAs(9, "PARENT"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 5000}"))
					.andExpect(status().isCreated());
		}

		@Test
		@DisplayName("경로와 본문의 childId 가 다르면 지급하지 않고 400 으로 막는다")
		void childId가_어긋나면_거부한다() throws Exception {
			mockMvc.perform(post(GRANT_URL, 1, 2).with(loginAs(9, "PARENT"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"childId\": 99, \"amount\": 5000}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			verify(pointGrantService, never()).grant(any(), any(), any(), any(Integer.class), any(), any());
		}

		@Test
		@DisplayName("가족 자녀가 아니면 403 NOT_FAMILY_CHILD 로 매핑한다")
		void 가족_자녀가_아니면_403이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.NOT_FAMILY_CHILD, "가족 자녀 아님"))
					.given(pointGrantService).grant(any(), any(), any(), any(Integer.class), any(), any());

			mockMvc.perform(post(GRANT_URL, 1, 2).with(loginAs(9, "PARENT"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 5000}"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("NOT_FAMILY_CHILD"));
		}

		@Test
		@DisplayName("금액이 0 이하면 서비스를 호출하지 않고 400 으로 막는다")
		void 잘못된_금액은_400이다() throws Exception {
			mockMvc.perform(post(GRANT_URL, 1, 2).with(loginAs(9, "PARENT"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": -100}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			verify(pointGrantService, never()).grant(any(), any(), any(), any(Integer.class), any(), any());
		}

		private PointChangeResult grantResult(Integer transactionId, int amount, int balanceAfter) {
			return new PointChangeResult(transactionId, 10,
					PointTransactionReason.GRANT, amount, balanceAfter, OffsetDateTime.now(), false);
		}
	}

	private PointChangeResult result(Integer transactionId, int amount, int balanceAfter) {
		return new PointChangeResult(transactionId, 10,
				PointTransactionReason.CHARGE, amount, balanceAfter, OffsetDateTime.now(), false);
	}

	/**
	 * 컨트롤러가 @AuthenticationPrincipal 로 읽는 주체를 심는다.
	 * 필터를 껐어도 인자 리졸버는 SecurityContext 를 보므로 이 방식이 필요하다.
	 */
	private static org.springframework.test.web.servlet.request.RequestPostProcessor loginAs(
			Integer userId, String role) {
		return authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				userId, null,
				java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
						"ROLE_" + role))));
	}
}
