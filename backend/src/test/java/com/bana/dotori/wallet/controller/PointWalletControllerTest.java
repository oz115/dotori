package com.bana.dotori.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
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
import com.bana.dotori.wallet.dto.response.PointBalanceResponse;
import com.bana.dotori.wallet.dto.response.CursorPage;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.dto.response.PointTransactionHistoryResponse;
import com.bana.dotori.wallet.dto.response.PointTransactionItem;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointCashoutRequestService;
import com.bana.dotori.wallet.service.PointChargeService;
import com.bana.dotori.wallet.service.PointGrantService;
import com.bana.dotori.wallet.service.PointWalletQueryService;

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
	private PointWalletQueryService pointWalletQueryService;

	@MockitoBean
	private PointCashoutRequestService pointCashoutRequestService;

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
	@DisplayName("보유 포인트 조회")
	class Balance {

		private static final String BALANCE_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/balance";

		@Test
		@DisplayName("조회에 성공하면 200 과 명세 형식의 응답을 돌려준다")
		void 조회_성공() throws Exception {
			given(pointWalletQueryService.getBalance(eq(1), eq(2), eq(2)))
					.willReturn(new PointBalanceResponse(2, 12_000, OffsetDateTime.now()));

			mockMvc.perform(get(BALANCE_URL, 1, 2).with(loginAs(2, "CHILD")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.childId").value(2))
					.andExpect(jsonPath("$.balance").value(12_000))
					.andExpect(jsonPath("$.updatedAt").exists());
		}

		@Test
		@DisplayName("경로의 familyId·childId 와 호출자를 그대로 서비스에 넘긴다")
		void 인자를_그대로_넘긴다() throws Exception {
			given(pointWalletQueryService.getBalance(eq(7), eq(3), eq(9)))
					.willReturn(new PointBalanceResponse(3, 500, OffsetDateTime.now()));

			mockMvc.perform(get(BALANCE_URL, 7, 3).with(loginAs(9, "PARENT")))
					.andExpect(status().isOk());

			// 대상자와 호출자를 뒤바꿔 넘기면 부모가 자기 지갑을 조회하게 된다.
			verify(pointWalletQueryService).getBalance(7, 3, 9);
		}

		@Test
		@DisplayName("지갑이 없으면 404 가 아니라 200 과 잔액 0 이다")
		void 지갑이_없어도_200이다() throws Exception {
			given(pointWalletQueryService.getBalance(eq(1), eq(2), eq(2)))
					.willReturn(PointBalanceResponse.empty(2));

			mockMvc.perform(get(BALANCE_URL, 1, 2).with(loginAs(2, "CHILD")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.balance").value(0))
					.andExpect(jsonPath("$.updatedAt").doesNotExist());
		}

		@Test
		@DisplayName("권한이 없으면 403 WALLET_ACCESS_DENIED 로 매핑한다")
		void 권한_없음은_403이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.WALLET_ACCESS_DENIED, "권한 없음"))
					.given(pointWalletQueryService).getBalance(any(), any(), any());

			mockMvc.perform(get(BALANCE_URL, 1, 3).with(loginAs(2, "CHILD")))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("WALLET_ACCESS_DENIED"))
					.andExpect(jsonPath("$.timestamp").exists());
		}

		@Test
		@DisplayName("childId 가 숫자가 아니면 400 으로 막는다")
		void 잘못된_경로변수는_400이다() throws Exception {
			mockMvc.perform(get(BALANCE_URL, 1, "abc").with(loginAs(2, "CHILD")))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			verify(pointWalletQueryService, never()).getBalance(any(), any(), any());
		}
	}

	@Nested
	@DisplayName("포인트 거래내역 조회")
	class Transactions {

		private static final String TX_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/transactions";

		@Test
		@DisplayName("조회에 성공하면 200 과 목록·잔액·커서를 돌려준다")
		void 조회_성공() throws Exception {
			given(pointWalletQueryService.getTransactions(
					eq(1), eq(2), eq(2), isNull(), isNull(), isNull(),
					eq(Sort.Direction.DESC), isNull(), eq(20)))
					.willReturn(new PointTransactionHistoryResponse(2, 6_000,
							CursorPage.of(List.of(new PointTransactionItem(101,
									PointTransactionReason.CHARGE, "IN", 1_000, 6_000,
									"포인트 충전", OffsetDateTime.now())), "next-cursor")));

			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.childId").value(2))
					.andExpect(jsonPath("$.balance").value(6_000))
					.andExpect(jsonPath("$.page.items[0].pointTransactionId").value(101))
					.andExpect(jsonPath("$.page.items[0].transactionType").value("CHARGE"))
					.andExpect(jsonPath("$.page.items[0].transactionDirection").value("IN"))
					.andExpect(jsonPath("$.page.items[0].description").value("포인트 충전"))
					.andExpect(jsonPath("$.page.nextCursor").value("next-cursor"))
					.andExpect(jsonPath("$.page.hasNext").value(true));
		}

		@Test
		@DisplayName("총 개수는 응답에 넣지 않는다")
		void 총_개수는_없다() {
			// 무한 스크롤은 총 개수를 쓰지 않는데, 주려면 매 스크롤마다 COUNT(*) 를 실행해야 한다.
			assertThat(PointTransactionHistoryResponse.class.getRecordComponents())
					.extracting(java.lang.reflect.RecordComponent::getName)
					.containsExactly("childId", "balance", "page");
			assertThat(CursorPage.class.getRecordComponents())
					.extracting(java.lang.reflect.RecordComponent::getName)
					.containsExactly("items", "nextCursor", "hasNext");
		}

		@Test
		@DisplayName("조회 조건을 그대로 서비스에 넘긴다")
		void 조건을_그대로_넘긴다() throws Exception {
			given(pointWalletQueryService.getTransactions(any(), any(), any(), any(), any(), any(), any(),
					any(), any(Integer.class)))
					.willReturn(PointTransactionHistoryResponse.empty(2));

			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD"))
							.param("startDate", "2026-09-01")
							.param("endDate", "2026-09-03")
							.param("type", "GRANT")
							.param("sort", "ASC")
							.param("cursor", "abc123")
							.param("size", "5"))
					.andExpect(status().isOk());

			verify(pointWalletQueryService).getTransactions(1, 2, 2,
					LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3),
					PointTransactionReason.GRANT, Sort.Direction.ASC, "abc123", 5);
		}

		@Test
		@DisplayName("조건을 생략하면 최신순 첫 묶음으로 조회한다")
		void 기본값으로_조회한다() throws Exception {
			given(pointWalletQueryService.getTransactions(any(), any(), any(), any(), any(), any(), any(),
					any(), any(Integer.class)))
					.willReturn(PointTransactionHistoryResponse.empty(2));

			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD")))
					.andExpect(status().isOk());

			verify(pointWalletQueryService).getTransactions(1, 2, 2,
					null, null, null, Sort.Direction.DESC, null, 20);
		}

		@Test
		@DisplayName("거래가 없으면 200 과 빈 배열이고 커서도 없다")
		void 빈_결과도_200이다() throws Exception {
			given(pointWalletQueryService.getTransactions(any(), any(), any(), any(), any(), any(), any(),
					any(), any(Integer.class)))
					.willReturn(PointTransactionHistoryResponse.empty(2));

			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.page.items").isArray())
					.andExpect(jsonPath("$.page.items").isEmpty())
					.andExpect(jsonPath("$.page.hasNext").value(false))
					.andExpect(jsonPath("$.page.nextCursor").doesNotExist())
					.andExpect(jsonPath("$.balance").value(0));
		}

		@Test
		@DisplayName("지원하지 않는 거래 유형은 서비스를 부르지 않고 400 으로 막는다")
		void 잘못된_유형은_400이다() throws Exception {
			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD"))
							.param("type", "NOT_A_REASON"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			verify(pointWalletQueryService, never()).getTransactions(any(), any(), any(), any(), any(), any(),
					any(), any(), any(Integer.class));
		}

		@Test
		@DisplayName("지원하지 않는 정렬 값은 400 으로 막는다")
		void 잘못된_정렬은_400이다() throws Exception {
			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD"))
							.param("sort", "SIDEWAYS"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}

		@Test
		@DisplayName("날짜 형식이 틀리면 400 으로 막는다")
		void 잘못된_날짜는_400이다() throws Exception {
			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD"))
							.param("startDate", "2026-13-45"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}

		@Test
		@DisplayName("커서가 깨졌으면 400 INVALID_CURSOR 로 매핑한다")
		void 깨진_커서는_400이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.INVALID_CURSOR, "커서 해석 실패"))
					.given(pointWalletQueryService).getTransactions(any(), any(), any(), any(), any(), any(),
							any(), any(), any(Integer.class));

			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD"))
							.param("cursor", "!!broken!!"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
		}

		@Test
		@DisplayName("기간이 뒤집히면 400 INVALID_DATE_RANGE 로 매핑한다")
		void 기간_역전은_400이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.INVALID_DATE_RANGE, "기간 역전"))
					.given(pointWalletQueryService).getTransactions(any(), any(), any(), any(), any(), any(),
							any(), any(), any(Integer.class));

			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD"))
							.param("startDate", "2026-09-05")
							.param("endDate", "2026-09-01"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
		}

		@Test
		@DisplayName("조회 건수가 잘못되면 400 INVALID_PAGE_REQUEST 로 매핑한다")
		void 잘못된_건수는_400이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.INVALID_PAGE_REQUEST, "size 초과"))
					.given(pointWalletQueryService).getTransactions(any(), any(), any(), any(), any(), any(),
							any(), any(), any(Integer.class));

			mockMvc.perform(get(TX_URL, 1, 2).with(loginAs(2, "CHILD"))
							.param("size", "1000"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_PAGE_REQUEST"));
		}

		@Test
		@DisplayName("권한이 없으면 403 WALLET_ACCESS_DENIED 로 매핑한다")
		void 권한_없음은_403이다() throws Exception {
			willThrow(new BusinessException(ErrorCode.WALLET_ACCESS_DENIED, "권한 없음"))
					.given(pointWalletQueryService).getTransactions(any(), any(), any(), any(), any(), any(),
							any(), any(), any(Integer.class));

			mockMvc.perform(get(TX_URL, 1, 3).with(loginAs(2, "CHILD")))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("WALLET_ACCESS_DENIED"));
		}
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
