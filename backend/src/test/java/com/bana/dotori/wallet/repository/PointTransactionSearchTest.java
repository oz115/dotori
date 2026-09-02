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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * Repository 계층 - 거래내역 조회 쿼리를 실제 DB 에서 검증한다.
 *
 * <p>여기가 보려는 것은 두 가지다.
 *
 * <ol>
 *   <li>기간 경계값. TIMESTAMPTZ 와 KST 날짜 사이의 환산이 하루씩 밀리기 쉽다.</li>
 *   <li>페이지 경계. 정렬이 유일하지 않으면 어떤 거래가 두 번 나오거나 빠지는데,
 *       단위 테스트로는 절대 드러나지 않고 실제 DB 에서만 재현된다.</li>
 * </ol>
 *
 * <p>거래 시각을 직접 지정해야 해서 엔티티 대신 네이티브 SQL 로 심는다.
 * {@code created_at} 은 엔티티가 생성 시점으로 채우기 때문이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 거래내역 조회 쿼리")
class PointTransactionSearchTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

		Page<PointTransaction> found = search(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), null, 0, 20);

		assertThat(found.getContent()).extracting(PointTransaction::getAmount)
				.as("시작일 0시와 종료일 마지막 순간까지 포함해야 한다")
				.containsExactlyInAnyOrder(100, 200, 300);
	}

	@Test
	@DisplayName("사유로 걸러 조회한다")
	void 사유로_필터링한다() {
		insert(PointTransactionReason.CHARGE, 100, "2026-09-02T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-09-02T11:00:00+09:00");
		insert(PointTransactionReason.PURCHASE, 300, "2026-09-02T12:00:00+09:00");

		Page<PointTransaction> found = search(null, null, PointTransactionReason.GRANT, 0, 20);

		assertThat(found.getContent()).extracting(PointTransaction::getAmount).containsExactly(200);
	}

	@Test
	@DisplayName("기간과 사유를 함께 주면 둘 다 만족하는 거래만 나온다")
	void 조건을_조합한다() {
		insert(PointTransactionReason.GRANT, 100, "2026-09-01T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-09-05T10:00:00+09:00");
		insert(PointTransactionReason.CHARGE, 300, "2026-09-01T11:00:00+09:00");

		Page<PointTransaction> found = search(
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), PointTransactionReason.GRANT, 0, 20);

		assertThat(found.getContent()).extracting(PointTransaction::getAmount)
				.as("단독 조회 결과의 교집합이어야 한다")
				.containsExactly(100);
	}

	@Test
	@DisplayName("ASC 와 DESC 가 서로 뒤집힌 순서를 낸다")
	void 정렬_방향이_반영된다() {
		insert(PointTransactionReason.GRANT, 100, "2026-09-01T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-09-02T10:00:00+09:00");
		insert(PointTransactionReason.GRANT, 300, "2026-09-03T10:00:00+09:00");

		assertThat(search(null, null, null, 0, 20, Sort.Direction.ASC).getContent())
				.extracting(PointTransaction::getAmount).containsExactly(100, 200, 300);
		assertThat(search(null, null, null, 0, 20, Sort.Direction.DESC).getContent())
				.extracting(PointTransaction::getAmount).containsExactly(300, 200, 100);
	}

	@Test
	@DisplayName("거래 시각이 모두 같아도 페이지를 넘기며 누락·중복이 없다")
	void 페이지_경계에서_누락도_중복도_없다() {
		// 시각이 전부 동일한 상황이 정렬 안정성의 최악 조건이다.
		// created_at 만으로 정렬하면 페이지마다 순서가 흔들려 여기서 깨진다.
		int total = 25;
		for (int i = 1; i <= total; i++) {
			insert(PointTransactionReason.GRANT, i, "2026-09-02T10:00:00+09:00");
		}

		List<Integer> collected = new ArrayList<>();
		int size = 7;
		for (int page = 0; page * size < total; page++) {
			search(null, null, null, page, size).getContent()
					.forEach(t -> collected.add(t.getAmount()));
		}

		assertThat(collected).as("페이지를 모두 이어 붙이면 전체와 같아야 한다").hasSize(total);
		assertThat(collected).as("중복이 있으면 안 된다").doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("전체 건수와 페이지 수를 함께 알려준다")
	void 페이지_메타데이터를_채운다() {
		for (int i = 1; i <= 12; i++) {
			insert(PointTransactionReason.GRANT, i, "2026-09-02T10:00:00+09:00");
		}

		Page<PointTransaction> first = search(null, null, null, 0, 5);

		assertThat(first.getTotalElements()).isEqualTo(12);
		assertThat(first.getTotalPages()).isEqualTo(3);
		assertThat(first.hasNext()).isTrue();
		assertThat(search(null, null, null, 2, 5).hasNext())
				.as("마지막 페이지에는 다음이 없다")
				.isFalse();
	}

	@Test
	@DisplayName("다른 지갑의 거래는 섞이지 않는다")
	void 다른_지갑의_거래는_안_나온다() {
		insert(PointTransactionReason.GRANT, 100, "2026-09-02T10:00:00+09:00");

		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-search-other");
		Integer otherWalletId = pointWalletRepository.save(
				PointWallet.openWithBalance(otherChildId, 0)).getWalletId();
		insertFor(otherWalletId, PointTransactionReason.GRANT, 999, "2026-09-02T10:00:00+09:00");

		assertThat(search(null, null, null, 0, 20).getContent())
				.extracting(PointTransaction::getAmount)
				.containsExactly(100);
		assertThat(pointTransactionRepository.search(otherWalletId, null, null, null,
				PageRequest.of(0, 20)).getContent())
				.extracting(PointTransaction::getAmount)
				.containsExactly(999);
	}

	@Test
	@DisplayName("거래가 없으면 빈 페이지가 나온다")
	void 거래가_없으면_빈_페이지다() {
		Page<PointTransaction> found = search(null, null, null, 0, 20);

		assertThat(found.getContent()).isEmpty();
		assertThat(found.getTotalElements()).isZero();
	}

	private Page<PointTransaction> search(LocalDate startDate, LocalDate endDate,
			PointTransactionReason reason, int page, int size) {
		return search(startDate, endDate, reason, page, size, Sort.Direction.ASC);
	}

	private Page<PointTransaction> search(LocalDate startDate, LocalDate endDate,
			PointTransactionReason reason, int page, int size, Sort.Direction direction) {
		Pageable pageable = PageRequest.of(page, size,
				Sort.by(direction, "createdAt", "pointTransactionId"));
		return pointTransactionRepository.search(
				walletId,
				startDate == null ? null : startDate.atStartOfDay(KST).toOffsetDateTime(),
				endDate == null ? null : endDate.plusDays(1).atStartOfDay(KST).toOffsetDateTime(),
				reason,
				pageable);
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
