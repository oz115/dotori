package com.bana.dotori.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.bana.dotori.wallet.exception.InsufficientPointException;
import com.bana.dotori.wallet.exception.InvalidPointAmountException;
import com.bana.dotori.wallet.exception.PointBalanceExceededException;
import com.bana.dotori.wallet.exception.WalletErrorCode;

/**
 * 1계층 - 잔액 계산 규칙만 검증한다.
 *
 * <p>스프링도 DB 도 없다. 이 테스트가 실패하면 원인은 무조건 {@link PointWallet} 안에 있다.
 * 아래 계층(서비스·동시성) 테스트가 같이 빨개졌더라도 여기부터 고치면 된다.
 */
@DisplayName("PointWallet 잔액 계산 규칙")
class PointWalletTest {

	private static final Integer USER_ID = 1;

	@Nested
	@DisplayName("포인트를 더할 때")
	class Deposit {

		@Test
		@DisplayName("잔액이 금액만큼 늘고 변경된 잔액을 돌려준다")
		void 잔액이_증가한다() {
			PointWallet wallet = PointWallet.openWithBalance(USER_ID, 1_000);

			int balanceAfter = wallet.deposit(500);

			assertThat(balanceAfter).as("deposit() 반환값").isEqualTo(1_500);
			assertThat(wallet.getBalance()).as("지갑에 반영된 잔액").isEqualTo(1_500);
		}

		@ParameterizedTest(name = "금액이 {0}이면 거부한다")
		@ValueSource(ints = {0, -1, -1_000, Integer.MIN_VALUE})
		@DisplayName("0 이하 금액은 InvalidPointAmountException 으로 거부한다")
		void 유효하지_않은_금액을_거부한다(int invalidAmount) {
			PointWallet wallet = PointWallet.openWithBalance(USER_ID, 1_000);

			assertThatThrownBy(() -> wallet.deposit(invalidAmount))
					.isInstanceOf(InvalidPointAmountException.class)
					.hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.INVALID_POINT_AMOUNT);

			assertThat(wallet.getBalance()).as("거부됐으므로 잔액은 그대로여야 한다").isEqualTo(1_000);
		}

		@Test
		@DisplayName("INTEGER 상한을 넘으면 오버플로 대신 PointBalanceExceededException 을 던진다")
		void 상한을_넘으면_거부한다() {
			PointWallet wallet = PointWallet.openWithBalance(USER_ID, Integer.MAX_VALUE - 10);

			assertThatThrownBy(() -> wallet.deposit(11))
					.isInstanceOf(PointBalanceExceededException.class)
					.hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.POINT_BALANCE_EXCEEDED);

			assertThat(wallet.getBalance())
					.as("오버플로로 음수가 되지 않아야 한다")
					.isEqualTo(Integer.MAX_VALUE - 10);
		}

		@Test
		@DisplayName("정확히 상한까지는 허용한다")
		void 상한까지는_허용한다() {
			PointWallet wallet = PointWallet.openWithBalance(USER_ID, Integer.MAX_VALUE - 10);

			int balanceAfter = wallet.deposit(10);

			assertThat(balanceAfter).isEqualTo(Integer.MAX_VALUE);
		}
	}

	@Nested
	@DisplayName("포인트를 뺄 때")
	class Withdraw {

		@Test
		@DisplayName("잔액이 금액만큼 줄고 변경된 잔액을 돌려준다")
		void 잔액이_감소한다() {
			PointWallet wallet = PointWallet.openWithBalance(USER_ID, 1_000);

			int balanceAfter = wallet.withdraw(300);

			assertThat(balanceAfter).as("withdraw() 반환값").isEqualTo(700);
			assertThat(wallet.getBalance()).as("지갑에 반영된 잔액").isEqualTo(700);
		}

		@Test
		@DisplayName("잔액과 같은 금액은 전액 차감을 허용한다")
		void 전액_차감을_허용한다() {
			PointWallet wallet = PointWallet.openWithBalance(USER_ID, 1_000);

			assertThat(wallet.withdraw(1_000)).isZero();
		}

		@Test
		@DisplayName("잔액보다 1이라도 크면 InsufficientPointException 으로 거부한다")
		void 잔액이_부족하면_거부한다() {
			PointWallet wallet = PointWallet.openWithBalance(USER_ID, 1_000);

			assertThatThrownBy(() -> wallet.withdraw(1_001))
					.isInstanceOf(InsufficientPointException.class)
					.hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.INSUFFICIENT_POINT);

			assertThat(wallet.getBalance()).as("거부됐으므로 잔액은 그대로여야 한다").isEqualTo(1_000);
		}

		@ParameterizedTest(name = "금액이 {0}이면 거부한다")
		@ValueSource(ints = {0, -1, -1_000})
		@DisplayName("0 이하 금액은 잔액 검사보다 먼저 InvalidPointAmountException 으로 거부한다")
		void 유효하지_않은_금액을_거부한다(int invalidAmount) {
			PointWallet wallet = PointWallet.openWithBalance(USER_ID, 1_000);

			assertThatThrownBy(() -> wallet.withdraw(invalidAmount))
					.as("음수 금액을 차감하면 잔액이 늘어나는 버그가 되므로 반드시 금액 검증이 먼저다")
					.isInstanceOf(InvalidPointAmountException.class);

			assertThat(wallet.getBalance()).isEqualTo(1_000);
		}
	}

	@Test
	@DisplayName("새 지갑은 잔액 0으로 열린다")
	void 새_지갑은_잔액이_0이다() {
		PointWallet wallet = PointWallet.openFor(USER_ID);

		assertThat(wallet.getBalance()).isZero();
		assertThat(wallet.getUserId()).isEqualTo(USER_ID);
	}
}
