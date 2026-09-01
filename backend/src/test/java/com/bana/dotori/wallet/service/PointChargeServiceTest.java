package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.wallet.client.BankTransferClient;
import com.bana.dotori.wallet.client.BankTransferResult;
import com.bana.dotori.wallet.domain.PointTransactionReason;
import com.bana.dotori.wallet.domain.PointTransactionType;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.exception.BankTransferFailedException;
import com.bana.dotori.wallet.exception.InsufficientAccountBalanceException;
import com.bana.dotori.wallet.exception.PointWalletNotFoundException;

/**
 * 서비스 계층 - 충전의 조립 순서를 검증한다.
 *
 * <p>여기가 실패하면 원인은 잔액 계산도 HTTP 매핑도 아니고
 * "포인트 발행과 송금을 어떤 순서로, 어떤 조건에 부르는가"다.
 */
@DisplayName("PointChargeService 충전 흐름")
@ExtendWith(MockitoExtension.class)
class PointChargeServiceTest {

	private static final Integer FAMILY_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final String KEY = "charge-001";

	@Mock
	private PointWalletService pointWalletService;

	@Mock
	private BankTransferClient bankTransferClient;

	@InjectMocks
	private PointChargeService pointChargeService;

	@Test
	@DisplayName("포인트를 CHARGE 사유로 발행한 뒤 송금한다")
	void 포인트_발행_후_송금한다() {
		given(pointWalletService.deposit(any())).willReturn(applied(10_000, 15_000));
		given(bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000))
				.willReturn(new BankTransferResult("MOCK-1", 10_000, null, null));

		PointChangeResult result = pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY);

		ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
		verify(pointWalletService).deposit(captor.capture());
		assertThat(captor.getValue().reason()).isEqualTo(PointTransactionReason.CHARGE);
		assertThat(captor.getValue().amount()).isEqualTo(10_000);
		assertThat(captor.getValue().idempotencyKey()).isEqualTo(KEY);
		assertThat(result.balanceAfter()).isEqualTo(15_000);

		// 검증을 모두 통과한 뒤에 돈을 움직여야 한다.
		InOrder inOrder = inOrder(pointWalletService, bankTransferClient);
		inOrder.verify(pointWalletService).deposit(any());
		inOrder.verify(bankTransferClient).transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000);
	}

	@Test
	@DisplayName("포인트 발행이 실패하면 송금을 시도조차 하지 않는다")
	void 포인트_발행_실패시_송금하지_않는다() {
		willThrow(new PointWalletNotFoundException(CHILD_ID)).given(pointWalletService).deposit(any());

		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.isInstanceOf(PointWalletNotFoundException.class);

		verify(bankTransferClient, never()).transferFromChildToParent(any(), any(), anyInt());
	}

	@Test
	@DisplayName("송금이 실패하면 예외가 그대로 전파돼 트랜잭션이 롤백된다")
	void 송금_실패는_전파된다() {
		given(pointWalletService.deposit(any())).willReturn(applied(10_000, 15_000));
		willThrow(new InsufficientAccountBalanceException(CHILD_ID, 5_000, 10_000))
				.given(bankTransferClient).transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000);

		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.as("여기서 예외를 삼키면 포인트만 발행되고 돈은 안 옮겨진다")
				.isInstanceOf(InsufficientAccountBalanceException.class);
	}

	@Test
	@DisplayName("이미 처리된 요청이면 송금을 다시 보내지 않는다")
	void 중복_요청은_송금하지_않는다() {
		given(pointWalletService.deposit(any())).willReturn(replayed(10_000, 15_000));

		PointChangeResult result = pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY);

		assertThat(result.replayed()).isTrue();
		verify(bankTransferClient, never()).transferFromChildToParent(any(), any(), anyInt());
	}

	@Test
	@DisplayName("이체 금액이 요청 금액과 다르면 실패로 처리한다")
	void 부분_송금은_실패다() {
		given(pointWalletService.deposit(any())).willReturn(applied(10_000, 15_000));
		given(bankTransferClient.transferFromChildToParent(FAMILY_ID, CHILD_ID, 10_000))
				.willReturn(new BankTransferResult("MOCK-1", 9_000, null, null));

		assertThatThrownBy(() -> pointChargeService.charge(FAMILY_ID, CHILD_ID, 10_000, KEY))
				.as("발행한 포인트와 옮겨진 돈이 다르면 성공이 아니다")
				.isInstanceOf(BankTransferFailedException.class);
	}

	private PointChangeResult applied(int amount, int balanceAfter) {
		return result(amount, balanceAfter, false);
	}

	private PointChangeResult replayed(int amount, int balanceAfter) {
		return result(amount, balanceAfter, true);
	}

	private PointChangeResult result(int amount, int balanceAfter, boolean replayed) {
		return new PointChangeResult(100, 10, PointTransactionType.DEPOSIT, PointTransactionReason.CHARGE,
				amount, balanceAfter, OffsetDateTime.now(), replayed);
	}
}
