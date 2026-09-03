package com.bana.dotori.wallet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.wallet.dto.request.TransactionCursor;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * Repository 계층 - 거래내역 커서 조회를 실제 DB 에서 검증한다.
 *
 * <p>여기가 보려는 것은 세 가지다.
 *
 * <ol>
 *   <li>기간 경계값. TIMESTAMPTZ 와 KST 날짜 사이의 환산이 하루씩 밀리기 쉽다.</li>
 *   <li>커서 왕복. 커서를 이어가며 끝까지 훑었을 때 누락도 중복도 없어야 한다.
 *       시각이 전부 같은 최악 조건에서만 드러나는 문제라 실제 DB 가 필요하다.</li>
 *   <li>스크롤 중 새 거래가 앞에 끼어들어도 이미 본 항목이 다시 나오지 않는 것.
 *       offset 방식을 커서로 바꾼 이유가 바로 이것이다.</li>
 * </ol>
 *
 * <p>거래 시각을 직접 지정해야 해서 엔티티 대신 네이티브 SQL 로 심는다.
 * {@code created_at} 은 엔티티가 생성 시점으로 채우기 때문이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 거래내역 커서 조회")
class PointTransactionSearchTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int NO_LIMIT = 1_000;

	@Autowired
	private PointTransactionRepository pointTransactionRepository;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer childId;
	private Integer walletId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-search");
		walletId = pointWalletRepository.save(PointWallet.openWithBalance(childId, 10_000)).getWalletId();
	}

	@Test
	@DisplayName("시작일과 종료일 당일 거래가 모두 포함된다")
	void 기간_경계값이_포함된다() {
		// 종료일 23:59:59.999 는 "23:59:59 로 자르기" 구현에서 빠지는 값이다.
		insert(PointTransactionReason.CHARGE, 100, "2026-09-01T00:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-09-02T12:00:00+09:00");
		insert(PointTransactionReason.GRANT, 300, "2026-09-03T23:59:59.999+09:00");
		// 범위 밖
		insert(PointTransactionReason.GRANT, 400, "2026-08-31T23:59:59+09:00");
		insert(PointTransactionReason.GRANT, 500, "2026-09-04T00:00:00+09:00");

		List<PointTransaction> found = search(
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), null, null, NO_LIMIT);

		assertThat(found).extracting(PointTransaction::getAmount)
				.as("시작일 0시와 종료일 마지막 순간까지 포함해야 한다")
				.containsExactly(100, 200, 300);
	}

	@Test
	@DisplayName("사유로 걸러 조회한다")
	void 사유로_필터링한다() {
		insert(PointTransactionReason.CHARGE, 100, "2026-09-02T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-09-02T11:00:00+09:00");
		insert(PointTransactionReason.PURCHASE, 300, "2026-09-02T12:00:00+09:00");

		List<PointTransaction> found = search(null, null, PointTransactionReason.GRANT, null, NO_LIMIT);

		assertThat(found).extracting(PointTransaction::getAmount).containsExactly(200);
	}

	@Test
	@DisplayName("기간과 사유를 함께 주면 둘 다 만족하는 거래만 나온다")
	void 조건을_조합한다() {
		insert(PointTransactionReason.GRANT, 100, "2026-09-01T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-09-05T10:00:00+09:00");
		insert(PointTransactionReason.CHARGE, 300, "2026-09-01T11:00:00+09:00");

		List<PointTransaction> found = search(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
				PointTransactionReason.GRANT, null, NO_LIMIT);

		assertThat(found).extracting(PointTransaction::getAmount)
				.as("단독 조회 결과의 교집합이어야 한다")
				.containsExactly(100);
	}

	@Test
	@DisplayName("ASC 와 DESC 가 서로 뒤집힌 순서를 낸다")
	void 정렬_방향이_반영된다() {
		insert(PointTransactionReason.GRANT, 100, "2026-09-01T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-09-02T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 300, "2026-09-03T10:00:00+09:00");

		assertThat(search(null, null, null, null, NO_LIMIT, Sort.Direction.ASC))
				.extracting(PointTransaction::getAmount).containsExactly(100, 200, 300);
		assertThat(search(null, null, null, null, NO_LIMIT, Sort.Direction.DESC))
				.extracting(PointTransaction::getAmount).containsExactly(300, 200, 100);
	}

	@Test
	@DisplayName("limit 만큼만 가져온다")
	void limit을_지킨다() {
		for (int i = 1; i <= 10; i++) {
			insert(PointTransactionReason.GRANT, i, "2026-09-02T10:00:00+09:00");
		}

		assertThat(search(null, null, null, null, 3)).hasSize(3);
	}

	@Test
	@DisplayName("거래 시각이 모두 같아도 커서를 이어가며 누락·중복 없이 전부 읽는다")
	void 커서로_끝까지_훑는다() {
		// 시각이 전부 동일한 상황이 커서 비교의 최악 조건이다.
		// created_at 만 비교하면 같은 시각 묶음이 매번 다시 나오거나 통째로 건너뛰어진다.
		int total = 25;
		for (int i = 1; i <= total; i++) {
			insert(PointTransactionReason.GRANT, i, "2026-09-02T10:00:00+09:00");
		}

		List<Integer> collected = new ArrayList<>();
		TransactionCursor cursor = null;
		int size = 7;
		for (int guard = 0; guard < 20; guard++) {
			List<PointTransaction> chunk = search(null, null, null, cursor, size);
			if (chunk.isEmpty()) {
				break;
			}
			chunk.forEach(t -> collected.add(t.getAmount()));
			PointTransaction last = chunk.get(chunk.size() - 1);
			cursor = new TransactionCursor(last.getCreatedAt(), last.getPointTransactionId());
		}

		assertThat(collected).as("이어 붙이면 전체와 같아야 한다").hasSize(total);
		assertThat(collected).as("중복이 있으면 안 된다").doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("스크롤 중 새 거래가 앞에 들어와도 이미 본 항목이 다시 나오지 않는다")
	void 스크롤_중_삽입에도_중복이_없다() {
		// offset 방식이었다면 여기서 깨진다. 새 거래가 #1 로 들어와 전체가 한 칸씩 밀리고,
		// OFFSET 3 은 이전의 3번째 항목을 다시 가리킨다.
		for (int i = 1; i <= 6; i++) {
			insert(PointTransactionReason.GRANT, i, "2026-09-02T10:0" + i + ":00+09:00");
		}

		List<PointTransaction> first = search(null, null, null, null, 3, Sort.Direction.DESC);
		assertThat(first).extracting(PointTransaction::getAmount).containsExactly(6, 5, 4);

		// 사용자가 스크롤하는 사이에 부모가 용돈을 준 상황
		insert(PointTransactionReason.GRANT, 999, "2026-09-02T11:00:00+09:00");

		PointTransaction last = first.get(first.size() - 1);
		List<PointTransaction> second = search(null, null, null,
				new TransactionCursor(last.getCreatedAt(), last.getPointTransactionId()),
				3, Sort.Direction.DESC);

		assertThat(second).extracting(PointTransaction::getAmount)
				.as("커서 지점 이후만 나와야 한다. 새로 들어온 999 도, 이미 본 4·5·6 도 없어야 한다")
				.containsExactly(3, 2, 1);
	}

	@Test
	@DisplayName("커서는 기간·사유 조건과 함께 동작한다")
	void 커서와_조건이_함께_적용된다() {
		insert(PointTransactionReason.GRANT, 100, "2026-09-02T10:00:00+09:00");
		insert(PointTransactionReason.CHARGE, 200, "2026-09-02T11:00:00+09:00");
		insert(PointTransactionReason.GRANT, 300, "2026-09-02T12:00:00+09:00");
		insert(PointTransactionReason.GRANT, 400, "2026-09-05T10:00:00+09:00");

		List<PointTransaction> first = search(
				LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2),
				PointTransactionReason.GRANT, null, 1, Sort.Direction.ASC);
		assertThat(first).extracting(PointTransaction::getAmount).containsExactly(100);

		PointTransaction last = first.get(0);
		List<PointTransaction> second = search(
				LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2),
				PointTransactionReason.GRANT,
				new TransactionCursor(last.getCreatedAt(), last.getPointTransactionId()),
				10, Sort.Direction.ASC);

		assertThat(second).extracting(PointTransaction::getAmount)
				.as("커서 이후이면서 기간·사유도 만족해야 한다")
				.containsExactly(300);
	}

	@Test
	@DisplayName("ASC 커서는 반대 방향으로 이어진다")
	void ASC_커서도_동작한다() {
		insert(PointTransactionReason.GRANT, 100, "2026-09-01T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-09-02T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 300, "2026-09-03T10:00:00+09:00");

		List<PointTransaction> first = search(null, null, null, null, 1, Sort.Direction.ASC);
		PointTransaction last = first.get(0);

		assertThat(search(null, null, null,
				new TransactionCursor(last.getCreatedAt(), last.getPointTransactionId()),
				10, Sort.Direction.ASC))
				.extracting(PointTransaction::getAmount)
				.containsExactly(200, 300);
	}

	@Test
	@DisplayName("다른 지갑의 거래는 섞이지 않는다")
	void 다른_지갑의_거래는_안_나온다() {
		insert(PointTransactionReason.GRANT, 100, "2026-09-02T10:00:00+09:00");

		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-search-other");
		Integer otherWalletId = pointWalletRepository.save(
				PointWallet.openWithBalance(otherChildId, 0)).getWalletId();
		insertFor(otherWalletId, PointTransactionReason.GRANT, 999, "2026-09-02T10:00:00+09:00");

		assertThat(search(null, null, null, null, NO_LIMIT))
				.extracting(PointTransaction::getAmount)
				.containsExactly(100);
		assertThat(pointTransactionRepository.search(otherWalletId, null, null, null,
				null, Sort.Direction.ASC, NO_LIMIT))
				.extracting(PointTransaction::getAmount)
				.containsExactly(999);
	}

	@Test
	@DisplayName("거래가 없으면 빈 목록이 나온다")
	void 거래가_없으면_빈_목록이다() {
		assertThat(search(null, null, null, null, NO_LIMIT)).isEmpty();
	}

	private List<PointTransaction> search(LocalDate startDate, LocalDate endDate,
			PointTransactionReason reason, TransactionCursor cursor, int limit) {
		return search(startDate, endDate, reason, cursor, limit, Sort.Direction.ASC);
	}

	private List<PointTransaction> search(LocalDate startDate, LocalDate endDate,
			PointTransactionReason reason, TransactionCursor cursor, int limit, Sort.Direction direction) {
		return pointTransactionRepository.search(
				walletId,
				startDate == null ? null : startDate.atStartOfDay(KST).toOffsetDateTime(),
				endDate == null ? null : endDate.plusDays(1).atStartOfDay(KST).toOffsetDateTime(),
				reason,
				cursor,
				direction,
				limit);
	}

	private void insert(PointTransactionReason reason, int amount, String createdAt) {
		insertFor(walletId, reason, amount, createdAt);
	}

	private void insertFor(Integer targetWalletId, PointTransactionReason reason, int amount, String createdAt) {
		jdbcTemplate.update("""
				INSERT INTO point_transactions
					(wallet_id, transaction_type, amount, balance_after, description, created_at)
				VALUES (?, ?, ?, ?, ?, ?)
				""", targetWalletId, reason.name(), amount, 10_000, reason.name() + " " + amount,
				OffsetDateTime.parse(createdAt));
	}
}
