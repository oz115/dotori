package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.support.ChoreTestFixture;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 5계층 - 완료 요청·승인의 동시 요청 상황을 실제 스레드로 재현한다 (CHORE-09·10·11·12).
 *
 * <p>여기서 깨지는 것은 비즈니스 규칙이 아니라 락 전략과 제약이다. 서비스 단위 테스트는
 * 리포지토리를 모킹하므로 "검사와 쓰기 사이의 틈"을 원리상 볼 수 없고, 그 틈은
 * 이 계층에서만 드러난다.
 *
 * <p>막는 장치가 세 겹이고 각각 다른 것을 막는다.
 * <ol>
 *   <li>{@code UX_CHORE_COMPLETION_REQUESTS_PENDING} 부분 유니크 인덱스 — 대기 중 요청 중복</li>
 *   <li>완료 요청 행의 {@code SELECT ... FOR UPDATE} — 승인·거절·취소가 서로를 덮어쓰는 것</li>
 *   <li>{@code point_transactions} 의 멱등키 유니크 + 지갑 행 락 — 주기당 이중 지급</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("동시 집안일 완료 요청과 승인")
class ChoreCompletionConcurrencyTest {

	private static final int REWARD_POINTS = 500;
	private static final int INITIAL_BALANCE = 1_000;

	@Autowired
	private ChoreCompletionService choreCompletionService;

	@Autowired
	private ChoreApprovalService choreApprovalService;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer parentId;
	private Integer childId;
	private Integer familyId;
	private Integer choreId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		parentId = WalletTestFixture.insertParent(jdbcTemplate, "chore-parent");
		childId = WalletTestFixture.insertChild(jdbcTemplate, "chore-child");
		familyId = WalletTestFixture.insertFamilyWithChild(jdbcTemplate, parentId, childId);
		choreId = ChoreTestFixture.insertTodayChore(jdbcTemplate, familyId, parentId, childId, REWARD_POINTS);
		pointWalletRepository.save(PointWallet.openWithBalance(childId, INITIAL_BALANCE));
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@Test
	@DisplayName("아이가 동시에 여러 번 요청해도 대기 중 요청은 한 건만 남고 나머지는 409다")
	void 동시_완료요청은_한_건만_남는다() throws InterruptedException {
		int threadCount = 20;
		AtomicInteger created = new AtomicInteger();
		AtomicInteger conflicted = new AtomicInteger();

		runConcurrently(threadCount, index -> {
			try {
				choreCompletionService.requestCompletion(childId, familyId, childId, choreId,
						new ChoreCompletionCreateRequest(null, "다 했어요 " + index));
				created.incrementAndGet();
			} catch (BusinessException e) {
				// 예상한 실패는 "이미 대기 중" 하나뿐이다. 다른 코드까지 여기서 삼키면
				// 유니크 위반이 500 으로 나가는 회귀가 "충돌 건수"에 섞여 드러나지 않는다.
				if (e.getErrorCode() != ErrorCode.PENDING_REQUEST_EXISTS) {
					throw e;
				}
				conflicted.incrementAndGet();
			}
		});

		assertThat(created.get()).as("대기 중 요청은 할 일당 하나여야 한다").isEqualTo(1);
		assertThat(conflicted.get()).as("나머지는 500 이 아니라 409 로 거부돼야 한다").isEqualTo(threadCount - 1);
		assertThat(ChoreTestFixture.countRequestsByStatus(jdbcTemplate, choreId, "PENDING"))
				.as("애플리케이션 검사에는 틈이 있어 DB 부분 유니크 인덱스가 최종적으로 막는다")
				.isEqualTo(1);
	}

	@Test
	@DisplayName("거절당한 뒤에는 같은 주기에 다시 요청할 수 있다")
	void 거절_뒤_같은_주기에_재요청된다() {
		Integer requestId = ChoreTestFixture.insertPendingRequest(jdbcTemplate, choreId, childId);
		choreCompletionService.rejectCompletion(parentId, familyId, childId, requestId);

		// 부분 유니크 인덱스가 PENDING 만 보므로 REJECTED 로 남은 행은 재요청을 막지 않는다.
		// 막는 것은 지급이지 요청이 아니다 — 부모가 잘못 눌렀을 때 되돌릴 길이 있어야 한다.
		choreCompletionService.requestCompletion(childId, familyId, childId, choreId,
				new ChoreCompletionCreateRequest(null, "다시 했어요"));

		assertThat(ChoreTestFixture.countRequestsByStatus(jdbcTemplate, choreId, "PENDING")).isEqualTo(1);
		assertThat(ChoreTestFixture.countRequestsByStatus(jdbcTemplate, choreId, "REJECTED")).isEqualTo(1);
	}

	@Test
	@DisplayName("같은 요청을 동시에 여러 번 승인해도 지급은 한 번이고 잔액이 두 배가 되지 않는다")
	void 동시_승인은_한_번만_지급한다() throws InterruptedException {
		Integer requestId = ChoreTestFixture.insertPendingRequest(jdbcTemplate, choreId, childId);
		int threadCount = 20;
		AtomicInteger approved = new AtomicInteger();
		AtomicInteger rejected = new AtomicInteger();

		runConcurrently(threadCount, index -> {
			try {
				choreApprovalService.approve(parentId, familyId, childId, requestId);
				approved.incrementAndGet();
			} catch (BusinessException e) {
				// INVALID_STATUS = 앞선 스레드가 이미 처리했다 (Jira 에는 400 으로 적혀 있지만 409 다).
				// ALREADY_REWARDED = 상태 전이는 통과했으나 지급 멱등키에 걸렸다.
				if (e.getErrorCode() != ErrorCode.INVALID_STATUS
						&& e.getErrorCode() != ErrorCode.ALREADY_REWARDED) {
					throw e;
				}
				rejected.incrementAndGet();
			}
		});

		assertThat(approved.get()).as("승인은 정확히 한 번만 성공해야 한다").isEqualTo(1);
		assertThat(rejected.get()).as("나머지는 모두 거부돼야 한다").isEqualTo(threadCount - 1);
		assertThat(ChoreTestFixture.selectRequestStatus(jdbcTemplate, requestId)).isEqualTo("SUCCEED");
		assertThat(ChoreTestFixture.countRewardTransactions(jdbcTemplate, childId))
				.as("원장에 CHORE_REWARD 가 두 줄이면 이중 지급이다")
				.isEqualTo(1);
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("잔액은 보상만큼만 늘어야 한다")
				.isEqualTo(INITIAL_BALANCE + REWARD_POINTS);
	}

	@Test
	@DisplayName("지급이 실패하면 요청 상태가 PENDING 으로 롤백되고 포인트도 남지 않는다")
	void 지급_실패는_상태까지_되돌린다() {
		Integer requestId = ChoreTestFixture.insertPendingRequest(jdbcTemplate, choreId, childId);
		// 잔액을 INTEGER 상한에 붙여 두면 deposit 이 POINT_BALANCE_EXCEEDED 로 터진다.
		// 모킹 없이 "지급 단계에서만 실패하는" 상황을 만드는 방법이다.
		jdbcTemplate.update("UPDATE point_wallets SET balance = ? WHERE child_id = ?",
				Integer.MAX_VALUE, childId);

		try {
			choreApprovalService.approve(parentId, familyId, childId, requestId);
			throw new AssertionError("지급이 실패해야 하는 상황인데 승인이 성공했다");
		} catch (BusinessException e) {
			assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POINT_BALANCE_EXCEEDED);
		}

		// 상태 전이와 지급이 같은 트랜잭션이라 함께 되돌아가야 한다. 나뉘어 있으면
		// "승인은 됐는데 포인트는 안 들어온" 행이 남고, 그건 되돌릴 방법이 없는 사고다.
		assertThat(ChoreTestFixture.selectRequestStatus(jdbcTemplate, requestId))
				.as("지급이 실패했으면 승인도 없던 일이 돼야 한다")
				.isEqualTo("PENDING");
		assertThat(ChoreTestFixture.countRewardTransactions(jdbcTemplate, childId))
				.as("실패한 지급의 원장 행이 남으면 안 된다")
				.isZero();
		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("잔액도 그대로여야 한다")
				.isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	@DisplayName("아이가 취소하는 동시에 부모가 승인하면 하나만 성공하고, 승인이 이겼을 때만 지급된다")
	void 취소와_승인이_겹치면_하나만_성공한다() throws InterruptedException {
		Integer requestId = ChoreTestFixture.insertPendingRequest(jdbcTemplate, choreId, childId);
		AtomicInteger approved = new AtomicInteger();
		AtomicInteger cancelled = new AtomicInteger();
		AtomicInteger rejected = new AtomicInteger();

		runConcurrently(2, index -> {
			try {
				if (index == 0) {
					choreApprovalService.approve(parentId, familyId, childId, requestId);
					approved.incrementAndGet();
				} else {
					choreCompletionService.cancelCompletion(childId, familyId, childId, requestId);
					cancelled.incrementAndGet();
				}
			} catch (BusinessException e) {
				if (e.getErrorCode() != ErrorCode.INVALID_STATUS) {
					throw e;
				}
				rejected.incrementAndGet();
			}
		});

		// 어느 쪽이 이기는지는 정해져 있지 않다. 정해져 있어야 하는 것은
		// "둘 다 성공하지 않는다" 와 "상태와 지급이 어긋나지 않는다" 두 가지다.
		assertThat(approved.get() + cancelled.get()).as("둘 다 성공하면 갱신 손실이다").isEqualTo(1);
		assertThat(rejected.get()).as("진 쪽은 409 로 거부돼야 한다").isEqualTo(1);

		String status = ChoreTestFixture.selectRequestStatus(jdbcTemplate, requestId);
		int rewards = ChoreTestFixture.countRewardTransactions(jdbcTemplate, childId);
		int balance = WalletTestFixture.selectBalance(jdbcTemplate, childId);

		if (approved.get() == 1) {
			assertThat(status).isEqualTo("SUCCEED");
			assertThat(rewards).as("승인이 이겼으면 지급돼 있어야 한다").isEqualTo(1);
			assertThat(balance).isEqualTo(INITIAL_BALANCE + REWARD_POINTS);
		} else {
			// 락이 없으면 여기서 깨진다 — 지급은 됐는데 상태가 CANCELLED 로 덮인다.
			assertThat(status).isEqualTo("CANCELLED");
			assertThat(rewards).as("취소가 이겼으면 포인트가 나가면 안 된다").isZero();
			assertThat(balance).isEqualTo(INITIAL_BALANCE);
		}
	}

	/**
	 * 모든 스레드를 같은 순간에 출발시켜 경합을 최대한 만든다.
	 *
	 * <p>{@code PointWalletServiceConcurrencyTest} 의 헬퍼와 같은 형태다. 작업 스레드에서 터진
	 * 예외는 모아서 다시 던진다 — {@code ExecutorService.submit} 이 예외를 Future 안에 가둬
	 * 버리므로, 그냥 두면 실패가 "카운트가 이상하다" 정도로만 드러나 원인을 찾기 어렵다.
	 * 테스트가 예상하는 예외는 task 안에서 직접 잡는다.
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
