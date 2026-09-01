package com.bana.dotori.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.wallet.domain.PointTransactionReason;
import com.bana.dotori.wallet.domain.PointTransactionType;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.exception.AccountNotLinkedException;
import com.bana.dotori.wallet.exception.InsufficientAccountBalanceException;
import com.bana.dotori.wallet.exception.InvalidPointAmountException;
import com.bana.dotori.wallet.exception.PointWalletNotFoundException;
import com.bana.dotori.wallet.service.PointChargeService;

/**
 * 컨트롤러 계층 - HTTP 경계만 검증한다.
 *
 * <p>서비스를 가짜로 대체했으므로 잔액 계산이나 송금은 전혀 일어나지 않는다.
 * 여기가 실패하면 원인은 요청 매핑, 검증 애노테이션, 예외 → 상태코드 매핑 중 하나다.
 *
 * <p>인증 필터를 끄고 돌린다. SecurityConfig 는 회원가입 담당 브랜치에 있어
 * 이 브랜치에는 아직 없고, 스프링 시큐리티 기본 설정이 모든 요청을 막기 때문이다.
 */
@WebMvcTest(PointWalletController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("포인트 충전 API")
class PointWalletControllerTest {

	private static final String CHARGE_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/charge";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PointChargeService pointChargeService;

	@Test
	@DisplayName("충전에 성공하면 201 과 명세 형식의 응답을 돌려준다")
	void 충전_성공() throws Exception {
		given(pointChargeService.charge(eq(1), eq(2), eq(10_000), eq("charge-001")))
				.willReturn(result(100, 10_000, 15_000));

		mockMvc.perform(post(CHARGE_URL, 1, 2)
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

		mockMvc.perform(post(CHARGE_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("금액이 0 이하면 서비스를 호출하지 않고 400 INVALID_REQUEST 로 막는다")
	void 잘못된_금액은_요청_검증에서_막힌다() throws Exception {
		mockMvc.perform(post(CHARGE_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verify(pointChargeService, never()).charge(any(), any(), any(Integer.class), any());
	}

	@Test
	@DisplayName("금액이 없으면 400 으로 막는다")
	void 금액_누락은_400이다() throws Exception {
		mockMvc.perform(post(CHARGE_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("계좌 잔액이 부족하면 400 INSUFFICIENT_ACCOUNT_BALANCE 로 매핑한다")
	void 계좌_잔액_부족은_400이다() throws Exception {
		willThrow(new InsufficientAccountBalanceException(2, 5_000, 10_000))
				.given(pointChargeService).charge(any(), any(), any(Integer.class), any());

		mockMvc.perform(post(CHARGE_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_ACCOUNT_BALANCE"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	@DisplayName("연동 계좌가 없으면 404 ACCOUNT_NOT_LINKED 로 매핑한다")
	void 계좌_미연동은_404다() throws Exception {
		willThrow(new AccountNotLinkedException("child", 2))
				.given(pointChargeService).charge(any(), any(), any(Integer.class), any());

		mockMvc.perform(post(CHARGE_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_LINKED"));
	}

	@Test
	@DisplayName("지갑이 없으면 404 WALLET_NOT_FOUND 로 매핑한다")
	void 지갑_없음은_404다() throws Exception {
		willThrow(new PointWalletNotFoundException(2))
				.given(pointChargeService).charge(any(), any(), any(Integer.class), any());

		mockMvc.perform(post(CHARGE_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));
	}

	@Test
	@DisplayName("서비스가 던진 도메인 예외도 각자의 코드로 매핑된다")
	void 도메인_예외가_코드로_매핑된다() throws Exception {
		willThrow(new InvalidPointAmountException(-1))
				.given(pointChargeService).charge(any(), any(), any(Integer.class), any());

		mockMvc.perform(post(CHARGE_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_POINT_AMOUNT"));
	}

	private PointChangeResult result(Integer transactionId, int amount, int balanceAfter) {
		return new PointChangeResult(transactionId, 10, PointTransactionType.DEPOSIT,
				PointTransactionReason.CHARGE, amount, balanceAfter, OffsetDateTime.now(), false);
	}
}
