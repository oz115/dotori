package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointTransactionRepository;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 5계층 - 동시 요청 상황을 실제 스레드로 재현한다.
 *
 * <p>다른 계층 테스트가 모두 통과해도 여기서만 깨질 수 있다.
 * 그 경우 원인은 비즈니스 규칙이 아니라 락 전략이나 멱등성 확인 순서다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("동시 포인트 요청")
class PointWalletServiceConcurrencyTest {

	private static final int INITIAL_BALANCE = 1_000;

	@Autowired
	private PointWalletService pointWalletService;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private PointTransactionRepository pointTransactionRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer childId;
	private Integer walletId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-concurrency");
		walletId = pointWalletRepository.save(PointWallet.openWithBalance(childId, INITIAL_BALANCE)).getWalletId();
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@Test
	@DisplayName("동시에 차감해도 잔액이 음수가 되지 않고 성공 건수가 잔액과 맞아떨어진다")
	void 동시_차감에도_잔액이_음수가_되지_않는다() throws InterruptedException {
		int threadCount = 20;
		int amountPerRequest = 100;
		int expectedSuccess = INITIAL_BALANCE / amountPerRequest;

		AtomicInteger succeeded = new AtomicInteger();
		AtomicInteger rejected = new AtomicInteger();

		runConcurrently(threadCount, index -> {
			try {
				pointWalletService.withdraw(PointChangeCommand.of(childId, amountPerRequest,
						PointTransactionReason.PURCHASE, "동시 구매 " + index, "concurrent-purchase-" + index));
				succeeded.incrementAndGet();
			} catch (BusinessException e) {
				// 잔액 부족만 예상한 실패다. 다른 코드까지 여기서 삼키면
				// 진짜 버그가 "거부 건수"에 섞여 들어가 드러나지 않는다.
				if (e.getErrorCode() != ErrorCode.INSUFFICIENT_POINT) {
					throw e;
				}
				rejected.incrementAndGet();
			}
		});

		assertThat(succeeded.get()).as("잔액으로 감당 가능한 만큼만 성공해야 한다").isEqualTo(expectedSuccess);
		assertThat(rejected.get()).as("나머지는 잔액 부족으로 거부돼야 한다").isEqualTo(threadCount - expectedSuccess);

		int finalBalance = WalletTestFixture.selectBalance(jdbcTemplate, childId);
		assertThat(finalBalance).as("잔액은 절대 음수가 될 수 없다").isNotNegative();
		assertThat(finalBalance).as("성공 건수와 잔액이 일치해야 한다").isZero();
	}

	@Test
	@DisplayName("같은 멱등키로 동시에 요청해도 단 한 번만 반영된다")
	void 동일_멱등키_동시요청은_한번만_반영된다() throws InterruptedException {
		int threadCount = 20;
		int amount = 500;
		String sharedKey = "charge-duplicated-request";

		AtomicInteger applied = new AtomicInteger();
		AtomicInteger replayed = new AtomicInteger();

		runConcurrently(threadCount, index -> {
			boolean isReplay = pointWalletService.deposit(PointChangeCommand.of(
					childId, amount, PointTransactionReason.CHARGE, "중복 충전", sharedKey)).replayed();
			if (isReplay) {
				replayed.incrementAndGet();
			} else {
				applied.incrementAndGet();
			}
		});

		assertThat(applied.get()).as("실제로 반영된 요청은 정확히 하나여야 한다").isEqualTo(1);
		assertThat(replayed.get()).as("나머지는 이전 거래를 그대로 돌려받아야 한다").isEqualTo(threadCount - 1);

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("중복 요청이 잔액을 여러 번 늘리면 안 된다")
				.isEqualTo(INITIAL_BALANCE + amount);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate))
				.as("원장에도 한 줄만 남아야 한다")
				.isEqualTo(1);
	}

	@Test
	@DisplayName("동시에 지급과 차감이 섞여 들어와도 원장 합계와 최종 잔액이 일치한다")
	void 지급과_차감이_섞여도_원장과_잔액이_일치한다() throws InterruptedException {
		int pairCount = 10;
		int depositAmount = 300;
		int withdrawAmount = 200;

		// 실행 순서는 보장되지 않는다. 차감이 지급보다 먼저 몰리면 잔액이 부족해
		// 일부 차감이 정당하게 실패하고, 그러면 이 테스트는 갱신 손실이 아닌 이유로 깨진다.
		// 검증하려는 것은 "동시 실행 중 갱신 손실이 없는가"뿐이므로,
		// 차감 총액을 미리 채워 두어 순서와 무관하게 모든 요청이 성공하도록 만든다.
		int topUp = pairCount * withdrawAmount;
		pointWalletService.deposit(PointChangeCommand.withoutIdempotency(
				childId, topUp, PointTransactionReason.GRANT, "동시성 테스트 준비"));

		runConcurrently(pairCount * 2, index -> {
			if (index % 2 == 0) {
				pointWalletService.deposit(PointChangeCommand.of(childId, depositAmount,
						PointTransactionReason.GRANT, "지급 " + index, "mixed-grant-" + index));
			} else {
				pointWalletService.withdraw(PointChangeCommand.of(childId, withdrawAmount,
						PointTransactionReason.PURCHASE, "구매 " + index, "mixed-purchase-" + index));
			}
		});

		int finalBalance = WalletTestFixture.selectBalance(jdbcTemplate, childId);
		int expected = INITIAL_BALANCE + topUp + (pairCount * depositAmount) - (pairCount * withdrawAmount);
		assertThat(finalBalance).as("갱신 손실이 있으면 이 값이 어긋난다").isEqualTo(expected);

		// 원장을 순서대로 훑으면서, 앞 거래의 잔액에서 이번 금액만큼만 움직였는지 본다.
		// 증감 방향은 따로 저장하지 않으므로 balance_after 의 변화량으로 판단한다.
		// 변화량의 절댓값이 amount 와 다르면 어딘가에서 갱신 손실이 난 것이다.
		List<PointTransaction> ledger =
				pointTransactionRepository.findAllByWalletIdOrderByPointTransactionIdAsc(walletId);
		int previousBalance = INITIAL_BALANCE;
		for (PointTransaction transaction : ledger) {
			int delta = transaction.getBalanceAfter() - previousBalance;
			assertThat(Math.abs(delta))
					.as("거래 %d 의 잔액 변화(%d)가 금액(%d)과 달라 원장이 깨졌다",
							transaction.getPointTransactionId(), delta, transaction.getAmount())
					.isEqualTo(transaction.getAmount());
			previousBalance = transaction.getBalanceAfter();
		}
		assertThat(previousBalance).as("원장 마지막 잔액과 실제 잔액이 일치해야 한다").isEqualTo(finalBalance);
		assertThat(ledger).as("준비용 지급 1건 + 동시 요청 건수").hasSize(pairCount * 2 + 1);
	}

	/**
	 * 모든 스레드를 같은 순간에 출발시켜 경합을 최대한 만든다.
	 *
	 * <p>작업 스레드에서 터진 예외는 반드시 모아서 다시 던진다.
	 * ExecutorService.submit 은 예외를 Future 안에 가둬 버리기 때문에,
	 * 그냥 두면 실패가 "최종 잔액이 이상하다" 정도로만 드러나 원인을 찾기 어렵다.
	 * 테스트가 예외를 예상하는 경우(잔액 부족 등)는 task 안에서 직접 잡는다.
	 */
	private void runConcurrently(int threadCount, java.util.function.IntConsumer task) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threadCount);
		Queue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

		for (int i = 0; i < threadCount; i++) {
			int index = i;
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					task.accept(index);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (RuntimeException | Error e) {
					unexpected.add(e);
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(10, TimeUnit.SECONDS);
		start.countDown();
		boolean finished = done.await(60, TimeUnit.SECONDS);
		executor.shutdownNow();
		assertThat(finished).as("동시 요청이 60초 안에 끝나지 않았다. 데드락이나 락 대기를 의심해야 한다").isTrue();

		if (!unexpected.isEmpty()) {
			throw new AssertionError(
					"작업 스레드 " + unexpected.size() + "개에서 예상치 못한 예외가 발생했다 (원인은 cause 참고)",
					unexpected.peek());
		}
	}
}
