package com.bana.dotori.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.report.dto.ReportTypeAmountRow;
import com.bana.dotori.report.dto.response.ReportDailyAmount;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * Repository 계층 - 리포트 집계를 실제 DB 에서 검증한다 (RPT-02·03).
 *
 * <p>팀 규칙은 리포지토리 테스트를 기본으로 만들지 않는 것이지만, 여기는 예외로 뒀다.
 * 일자별 집계가 {@code (created_at AT TIME ZONE 'Asia/Seoul')::date} 네이티브 SQL 이고,
 * <b>이 환산이 틀리면 한국 시간 자정부터 오전 9 시 사이의 거래가 전날로 집계된다.</b>
 * 목으로는 절대 잡히지 않고 PostgreSQL 없이는 검증할 방법이 없다 — H2 의 PostgreSQL MODE
 * 도 TIMESTAMPTZ 환산을 흉내 내지 못한다.
 *
 * <p>거래 시각을 직접 지정해야 해서 엔티티 대신 네이티브 SQL 로 심는다.
 * {@code created_at} 은 엔티티가 생성 시점으로 채우기 때문이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 리포트 집계 조회")
class PointReportQueryRepositoryTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Autowired
	private PointReportQueryRepository pointReportQueryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer childId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-report");
	}

	@Test
	@DisplayName("KST 자정 직후 거래는 그날로 집계된다 — UTC 로 자르면 전날로 밀린다")
	void 자정_직후_거래는_그날이다() {
		// UTC 로는 2026-08-13T15:30Z 다. UTC 기준으로 날짜를 떼면 8/13 으로 집계된다.
		insert(PointTransactionReason.ALLOWANCE, 3_000, "2026-08-14T00:30:00+09:00");

		List<ReportDailyAmount> byDate = aggregateIncomeByDate(
				LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 14));

		assertThat(byDate).hasSize(1);
		assertThat(byDate.get(0).date()).isEqualTo(LocalDate.of(2026, 8, 14));
		assertThat(byDate.get(0).amount()).isEqualTo(3_000L);
	}

	@Test
	@DisplayName("KST 오전 9시 이전 거래들이 전날로 새지 않는다")
	void 오전_9시_이전_거래가_전날로_새지_않는다() {
		// UTC 환산 시 전날이 되는 구간(KST 00:00~08:59)을 촘촘히 덮는다.
		insert(PointTransactionReason.GRANT, 100, "2026-08-14T00:00:00+09:00");
		insert(PointTransactionReason.GRANT, 200, "2026-08-14T05:00:00+09:00");
		insert(PointTransactionReason.GRANT, 400, "2026-08-14T08:59:59+09:00");
		// 이쪽은 UTC 로도 같은 날이다.
		insert(PointTransactionReason.GRANT, 800, "2026-08-14T23:00:00+09:00");

		List<ReportDailyAmount> byDate = aggregateIncomeByDate(
				LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 14));

		assertThat(byDate).hasSize(1);
		assertThat(byDate.get(0).date()).isEqualTo(LocalDate.of(2026, 8, 14));
		assertThat(byDate.get(0).amount()).isEqualTo(1_500L);
	}

	@Test
	@DisplayName("KST 자정 직전과 직후는 서로 다른 날로 갈린다")
	void 자정_경계에서_날짜가_갈린다() {
		insert(PointTransactionReason.ALLOWANCE, 1_000, "2026-08-14T23:59:59+09:00");
		insert(PointTransactionReason.ALLOWANCE, 2_000, "2026-08-15T00:00:00+09:00");

		List<ReportDailyAmount> byDate = aggregateIncomeByDate(
				LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 15));

		assertThat(byDate).hasSize(2);
		assertThat(byDate.get(0).date()).isEqualTo(LocalDate.of(2026, 8, 14));
		assertThat(byDate.get(0).amount()).isEqualTo(1_000L);
		assertThat(byDate.get(1).date()).isEqualTo(LocalDate.of(2026, 8, 15));
		assertThat(byDate.get(1).amount()).isEqualTo(2_000L);
	}

	@Test
	@DisplayName("거래가 없는 날은 행을 만들지 않는다")
	void 빈_날은_행이_없다() {
		insert(PointTransactionReason.ALLOWANCE, 1_000, "2026-08-01T12:00:00+09:00");
		insert(PointTransactionReason.ALLOWANCE, 2_000, "2026-08-05T12:00:00+09:00");

		List<ReportDailyAmount> byDate = aggregateIncomeByDate(
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

		assertThat(byDate).extracting(ReportDailyAmount::date)
				.containsExactly(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5));
	}

	@Test
	@DisplayName("시작일과 종료일 당일 거래가 모두 포함된다")
	void 기간_양끝이_포함된다() {
		insert(PointTransactionReason.ALLOWANCE, 1_000, "2026-08-01T00:00:00+09:00");
		insert(PointTransactionReason.ALLOWANCE, 2_000, "2026-08-31T23:59:59+09:00");
		// 기간 밖 — 종료일 다음 날 0 시는 반개구간의 끝이라 빠져야 한다.
		insert(PointTransactionReason.ALLOWANCE, 4_000, "2026-09-01T00:00:00+09:00");
		insert(PointTransactionReason.ALLOWANCE, 8_000, "2026-07-31T23:59:59+09:00");

		List<ReportTypeAmountRow> byType = aggregateIncomeByReason(
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

		assertThat(byType).hasSize(1);
		assertThat(byType.get(0).amount()).isEqualTo(3_000L);
		assertThat(byType.get(0).count()).isEqualTo(2L);
	}

	@Test
	@DisplayName("수입 집계는 소비 사유를 집계하지 않는다")
	void 수입_집계는_소비를_빼고_센다() {
		insert(PointTransactionReason.ALLOWANCE, 12_000, "2026-08-10T12:00:00+09:00");
		insert(PointTransactionReason.PURCHASE, 9_000, "2026-08-10T13:00:00+09:00");
		insert(PointTransactionReason.SAVINGS_DEPOSIT, 15_000, "2026-08-10T14:00:00+09:00");

		List<ReportTypeAmountRow> byType = aggregateIncomeByReason(
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

		assertThat(byType).extracting(ReportTypeAmountRow::reason)
				.containsExactly(PointTransactionReason.ALLOWANCE);
		assertThat(byType.get(0).amount()).isEqualTo(12_000L);
	}

	@Test
	@DisplayName("소비 집계는 수입 사유를 집계하지 않는다")
	void 소비_집계는_수입을_빼고_센다() {
		insert(PointTransactionReason.ALLOWANCE, 12_000, "2026-08-10T12:00:00+09:00");
		insert(PointTransactionReason.PURCHASE, 9_000, "2026-08-10T13:00:00+09:00");
		insert(PointTransactionReason.CASHOUT, 1_000, "2026-08-10T14:00:00+09:00");

		List<ReportTypeAmountRow> byType = pointReportQueryRepository.aggregateByReason(
				childId, startOf(LocalDate.of(2026, 8, 1)), startOf(LocalDate.of(2026, 9, 1)),
				PointTransactionReason.withdrawalReasons());

		assertThat(byType).extracting(ReportTypeAmountRow::reason)
				.containsExactly(PointTransactionReason.PURCHASE, PointTransactionReason.CASHOUT);
		assertThat(byType).extracting(ReportTypeAmountRow::amount)
				.containsExactly(9_000L, 1_000L);
	}

	@Test
	@DisplayName("사유별 집계는 금액 내림차순으로 나온다")
	void 금액_내림차순으로_정렬된다() {
		insert(PointTransactionReason.QUIZ_REWARD, 3_000, "2026-08-10T12:00:00+09:00");
		insert(PointTransactionReason.ALLOWANCE, 12_000, "2026-08-10T13:00:00+09:00");
		insert(PointTransactionReason.CHORE_REWARD, 10_000, "2026-08-10T14:00:00+09:00");

		List<ReportTypeAmountRow> byType = aggregateIncomeByReason(
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

		assertThat(byType).extracting(ReportTypeAmountRow::reason).containsExactly(
				PointTransactionReason.ALLOWANCE,
				PointTransactionReason.CHORE_REWARD,
				PointTransactionReason.QUIZ_REWARD);
	}

	@Test
	@DisplayName("다른 자녀의 거래는 섞이지 않는다")
	void 남의_거래는_섞이지_않는다() {
		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-report-other");
		insert(PointTransactionReason.ALLOWANCE, 1_000, "2026-08-10T12:00:00+09:00");
		insertFor(otherChildId, PointTransactionReason.ALLOWANCE, 9_000, "2026-08-10T12:00:00+09:00");

		List<ReportTypeAmountRow> byType = aggregateIncomeByReason(
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

		assertThat(byType).hasSize(1);
		assertThat(byType.get(0).amount()).isEqualTo(1_000L);
	}

	@Test
	@DisplayName("거래가 없으면 빈 목록이다")
	void 거래가_없으면_빈_목록이다() {
		assertThat(aggregateIncomeByReason(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
				.isEmpty();
		assertThat(aggregateIncomeByDate(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
				.isEmpty();
	}

	private List<ReportTypeAmountRow> aggregateIncomeByReason(LocalDate from, LocalDate to) {
		return pointReportQueryRepository.aggregateByReason(childId,
				startOf(from), startOf(to.plusDays(1)), PointTransactionReason.depositReasons());
	}

	private List<ReportDailyAmount> aggregateIncomeByDate(LocalDate from, LocalDate to) {
		return pointReportQueryRepository.aggregateByDate(childId,
				startOf(from), startOf(to.plusDays(1)), PointTransactionReason.depositReasons());
	}

	private OffsetDateTime startOf(LocalDate date) {
		return date.atStartOfDay(KST).toOffsetDateTime();
	}

	private void insert(PointTransactionReason reason, int amount, String createdAt) {
		insertFor(childId, reason, amount, createdAt);
	}

	private void insertFor(Integer targetChildId, PointTransactionReason reason, int amount,
			String createdAt) {

		jdbcTemplate.update("""
				INSERT INTO point_transactions
					(child_id, transaction_type, amount, balance_after, description, created_at)
				VALUES (?, ?, ?, ?, ?, ?)
				""", targetChildId, reason.name(), amount, 10_000, reason.name() + " " + amount,
				OffsetDateTime.parse(createdAt));
	}
}
