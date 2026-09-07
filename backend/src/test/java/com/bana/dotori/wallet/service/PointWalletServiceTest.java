package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.repository.PointTransactionRepository;
import com.bana.dotori.wallet.repository.PointWalletRepository;

/**
 * 2계층 - 서비스가 흐름을 올바르게 조립했는지 검증한다.
 *
 * <p>DB 없이 저장소를 가짜로 대체하므로, 여기가 실패하면 원인은 잔액 계산이 아니라
 * "순서·분기·저장소 호출" 쪽이다. 잔액 계산 자체는 {@code PointWalletTest} 가 담당한다.
 */
@DisplayName("PointWalletService 처리 흐름")
@ExtendWith(MockitoExtension.class)
class PointWalletServiceTest {

	private static final Integer CHILD_ID = 1;
	private static final String IDEMPOTENCY_KEY = "charge-20260901-0001";

	@Mock
	private PointWalletRepository pointWalletRepository;

	@Mock
	private PointTransactionRepository pointTransactionRepository;

	@InjectMocks
	private PointWalletService pointWalletService;

	private PointWallet wallet;

	@BeforeEach
	void setUp() {
		wallet = PointWallet.openWithBalance(CHILD_ID, 1_000);
	}

	@Nested
	@DisplayName("지급할 때")
	class Deposit {

		@Test
		@DisplayName("잔액을 늘리고 같은 트랜잭션에서 거래이력을 저장한다")
		void 잔액과_이력을_함께_남긴다() {
			given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));
			given(pointTransactionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());

			PointChangeResult result = pointWalletService.deposit(command(500));

			assertThat(wallet.getBalance()).as("변경된 잔액").isEqualTo(1_500);
			assertThat(result.balanceAfter()).as("결과에 실린 잔액").isEqualTo(1_500);
			assertThat(result.replayed()).as("새로 처리된 요청이다").isFalse();
			verify(pointTransactionRepository).saveAndFlush(any(PointTransaction.class));
		}

		@Test
		@DisplayName("멱등키가 이미 처리됐으면 잔액을 건드리지 않고 이전 거래를 그대로 돌려준다")
		void 중복_요청은_재반영하지_않는다() {
			PointTransaction alreadyProcessed = PointTransaction.record(CHILD_ID,
					PointTransactionReason.CHARGE, 500, 1_500, "충전", IDEMPOTENCY_KEY);
			given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));
			given(pointTransactionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY))
					.willReturn(Optional.of(alreadyProcessed));

			PointChangeResult result = pointWalletService.deposit(command(500));

			assertThat(result.replayed()).as("중복 요청임을 호출자가 알 수 있어야 알림 재발송을 건너뛴다").isTrue();
			assertThat(wallet.getBalance()).as("잔액은 두 번 반영되면 안 된다").isEqualTo(1_000);
			verify(pointTransactionRepository, never()).saveAndFlush(any(PointTransaction.class));
		}

		@Test
		@DisplayName("멱등성 확인은 반드시 지갑 락을 잡은 뒤에 한다")
		void 락을_먼저_잡는다() {
			given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));
			given(pointTransactionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());

			pointWalletService.deposit(command(500));

			// 순서가 뒤집히면 동시에 들어온 같은 요청이 둘 다 "처리된 적 없음"으로 판정돼 중복 반영된다.
			InOrder inOrder = inOrder(pointWalletRepository, pointTransactionRepository);
			inOrder.verify(pointWalletRepository).findByChildIdForUpdate(CHILD_ID);
			inOrder.verify(pointTransactionRepository)
					.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY);
		}

		@Test
		@DisplayName("멱등키가 없으면 중복 조회 없이 바로 처리한다")
		void 멱등키가_없으면_조회하지_않는다() {
			given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));

			pointWalletService.deposit(PointChangeCommand.withoutIdempotency(
					CHILD_ID, 500, PointTransactionReason.ALLOWANCE, "정기 지급"));

			verify(pointTransactionRepository, never()).findByChildIdAndIdempotencyKey(any(), anyString());
			verify(pointTransactionRepository).saveAndFlush(any(PointTransaction.class));
		}
	}

	@Nested
	@DisplayName("차감할 때")
	class Withdraw {

		@Test
		@DisplayName("잔액을 줄이고 WITHDRAWAL 이력을 남긴다")
		void 잔액과_이력을_함께_남긴다() {
			given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));
			given(pointTransactionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());

			PointChangeResult result = pointWalletService.withdraw(command(300));

			assertThat(wallet.getBalance()).isEqualTo(700);
			assertThat(result.balanceAfter()).isEqualTo(700);
		}

		@Test
		@DisplayName("잔액이 부족하면 이력을 저장하지 않고 InsufficientPointException 을 던진다")
		void 잔액이_부족하면_이력도_남기지_않는다() {
			given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));
			given(pointTransactionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());

			assertThatThrownBy(() -> pointWalletService.withdraw(command(1_001)))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINT);

			verify(pointTransactionRepository, never()).saveAndFlush(any(PointTransaction.class));
		}
	}

	@Test
	@DisplayName("지갑이 없으면 PointWalletNotFoundException 을 던진다")
	void 지갑이_없으면_실패한다() {
		given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> pointWalletService.deposit(command(500)))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_NOT_FOUND);

		verify(pointTransactionRepository, never()).saveAndFlush(any(PointTransaction.class));
	}

	@Test
	@DisplayName("0 이하 금액은 지갑을 잠근 뒤라도 이력 없이 거부한다")
	void 유효하지_않은_금액을_거부한다() {
		given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));
		given(pointTransactionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());

		assertThatThrownBy(() -> pointWalletService.deposit(command(0)))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POINT_AMOUNT);

		verify(pointTransactionRepository, never()).saveAndFlush(any(PointTransaction.class));
	}

	@Test
	@DisplayName("멱등키 유니크 위반은 DUPLICATE_POINT_REQUEST 로 바꿔 던진다")
	void 멱등키_위반을_409로_바꾼다() {
		given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));
		given(pointTransactionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY))
				.willReturn(Optional.empty());
		// JPA 경로에서 유니크 위반은 DuplicateKeyException 이 아니라 이 타입으로 올라온다.
		// 하위 타입을 잡으면 이 예외가 그대로 새어나가 500 이 된다.
		willThrow(new DataIntegrityViolationException(
				"""
				could not execute statement; SQL [n/a]; \
				constraint [UQ_POINT_TRANSACTIONS_CHILD_IDEMPOTENCY_KEY]"""))
				.given(pointTransactionRepository).saveAndFlush(any(PointTransaction.class));

		assertThatThrownBy(() -> pointWalletService.deposit(command(500)))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_POINT_REQUEST);
	}

	@Test
	@DisplayName("멱등키와 무관한 무결성 위반은 삼키지 않고 그대로 올려보낸다")
	void 다른_무결성_위반은_그대로_전파한다() {
		given(pointWalletRepository.findByChildIdForUpdate(CHILD_ID)).willReturn(Optional.of(wallet));
		given(pointTransactionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY))
				.willReturn(Optional.empty());
		willThrow(new DataIntegrityViolationException(
				"constraint [fk_point_transactions_wallet]"))
				.given(pointTransactionRepository).saveAndFlush(any(PointTransaction.class));

		assertThatThrownBy(() -> pointWalletService.deposit(command(500)))
				.as("외래키·CHECK 위반까지 409 로 둔갑하면 진짜 원인을 못 찾는다")
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private PointChangeCommand command(int amount) {
		return PointChangeCommand.of(CHILD_ID, amount, PointTransactionReason.CHARGE, "테스트 거래", IDEMPOTENCY_KEY);
	}
}
