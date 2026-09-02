package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * 서비스 계층 - 차감 파사드가 사유와 방향을 제대로 지키는지 본다.
 *
 * <p>여기가 실패하면 원인은 잔액 계산이 아니라 "어떤 사유의 차감을 허용하는가"다.
 */
@DisplayName("PointDeductionService 차감 흐름")
@ExtendWith(MockitoExtension.class)
class PointDeductionServiceTest {

	private static final Integer CHILD_ID = 2;

	@Mock
	private PointWalletService pointWalletService;

	@InjectMocks
	private PointDeductionService pointDeductionService;

	@ParameterizedTest(name = "{0} 사유는 차감할 수 있다")
	@EnumSource(value = PointTransactionReason.class,
			names = {"PURCHASE", "CASHOUT", "SAVINGS", "INVESTMENT"})
	@DisplayName("차감이 허용된 사유는 공통 서비스로 넘긴다")
	void 허용된_사유는_차감된다(PointTransactionReason reason) {
		given(pointWalletService.withdraw(any())).willReturn(result(3_000, 17_000));

		PointChangeResult result = pointDeductionService.deduct(
				CHILD_ID, 3_000, reason, "구매", "purchase-1");

		ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
		verify(pointWalletService).withdraw(captor.capture());
		assertThat(captor.getValue().reason()).isEqualTo(reason);
		assertThat(captor.getValue().amount()).isEqualTo(3_000);
		assertThat(result.balanceAfter()).isEqualTo(17_000);
	}

	@ParameterizedTest(name = "{0} 사유로는 차감할 수 없다")
	@EnumSource(value = PointTransactionReason.class,
			names = {"CHARGE", "GRANT", "ALLOWANCE", "QUIZ", "CHORE", "REFUND"})
	@DisplayName("지급 전용 사유로 차감을 요청하면 잔액을 건드리지 않고 막는다")
	void 지급_사유로는_차감할_수_없다(PointTransactionReason depositOnlyReason) {
		assertThatThrownBy(() -> pointDeductionService.deduct(
				CHILD_ID, 3_000, depositOnlyReason, "잘못된 호출", "wrong-1"))
				.as("이걸 통과시키면 원장에 '지급인데 잔액이 줄어든' 줄이 남는다")
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRANSACTION_REASON);

		verify(pointWalletService, never()).withdraw(any());
	}

	@Test
	@DisplayName("잔액이 부족하면 공통 서비스의 예외가 그대로 전파된다")
	void 잔액_부족은_전파된다() {
		willThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT, "잔액 부족")).given(pointWalletService).withdraw(any());

		assertThatThrownBy(() -> pointDeductionService.deduct(
				CHILD_ID, 3_000, PointTransactionReason.PURCHASE, "구매", "purchase-1"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINT);
	}

	@Test
	@DisplayName("환급은 REFUND 사유로 되돌려 준다")
	void 환급은_반대_거래를_남긴다() {
		given(pointWalletService.deposit(any())).willReturn(result(3_000, 20_000));

		pointDeductionService.refund(CHILD_ID, 3_000, "구매 취소", "refund-1");

		ArgumentCaptor<PointChangeCommand> captor = ArgumentCaptor.forClass(PointChangeCommand.class);
		verify(pointWalletService).deposit(captor.capture());
		assertThat(captor.getValue().reason())
				.as("차감 이력을 지우는 대신 반대 거래를 쌓아야 추적할 수 있다")
				.isEqualTo(PointTransactionReason.REFUND);
	}

	@Test
	@DisplayName("사유의 허용 방향 정의가 실제 업무와 맞는다")
	void 사유별_방향_정의가_맞는다() {
		// 예적금과 투자는 양방향이다. 가입·매수는 차감이고 만기 수령·매도는 지급이기 때문이다.
		assertThat(PointTransactionReason.SAVINGS.isDepositAllowed()).isTrue();
		assertThat(PointTransactionReason.SAVINGS.isWithdrawalAllowed()).isTrue();
		assertThat(PointTransactionReason.INVESTMENT.isDepositAllowed()).isTrue();
		assertThat(PointTransactionReason.INVESTMENT.isWithdrawalAllowed()).isTrue();

		// 나머지는 한 방향만 허용한다.
		assertThat(PointTransactionReason.CHARGE.isWithdrawalAllowed()).isFalse();
		assertThat(PointTransactionReason.GRANT.isWithdrawalAllowed()).isFalse();
		assertThat(PointTransactionReason.PURCHASE.isDepositAllowed()).isFalse();
		assertThat(PointTransactionReason.CASHOUT.isDepositAllowed()).isFalse();
	}

	private PointChangeResult result(int amount, int balanceAfter) {
		return new PointChangeResult(200, 10,
				PointTransactionReason.PURCHASE, amount, balanceAfter, OffsetDateTime.now(), false);
	}
}
