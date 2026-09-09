package com.bana.dotori.chore.support;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 집안일 테스트가 필요로 하는 최소한의 데이터를 만든다.
 *
 * <p>회원·가족은 {@code WalletTestFixture} 가 이미 만들어 주므로 여기서는 집안일과 완료 요청만
 * 다룬다. 뒷정리도 그쪽 {@code cleanUp} 이 함께 지운다 — 삭제 목록이 한 곳에 있어야
 * users 를 참조하는 테이블이 늘어날 때 FK 위반으로 깨지는 것을 막을 수 있다.
 *
 * <p>ENUM 컬럼에 문자열을 그대로 넣는다. JDBC URL 의 {@code ?stringtype=unspecified} 덕에
 * PostgreSQL 이 알아서 캐스팅한다 (지갑 픽스처가 {@code users.role} 을 넣는 방식과 같다).
 */
public final class ChoreTestFixture {

	private ChoreTestFixture() {
	}

	/**
	 * 오늘 수행할 수 있는 매일 반복 집안일을 만든다.
	 *
	 * <p>기간을 Java 에서 계산한 KST 날짜로 넣는다. {@code CURRENT_DATE} 를 쓰면 DB 세션
	 * 타임존에 따라 날짜가 하루 어긋나고, 그러면 완료 요청이 "수행 기간이 아닙니다" 로
	 * 막혀 테스트가 검증하려던 것과 무관한 이유로 깨진다.
	 *
	 * @param rewardPoints AI 산정이 끝난 상태를 흉내 내는 값. null 이면 완료 요청이 막힌다
	 */
	public static Integer insertTodayChore(JdbcTemplate jdbcTemplate, Integer familyId, Integer parentId,
			Integer childId, Integer rewardPoints) {
		LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
		return jdbcTemplate.queryForObject("""
				INSERT INTO chores (family_id, parent_id, child_id, title, difficulty, reward_points,
					repeat_type, start_date, end_date)
				VALUES (?, ?, ?, '설거지', 'NORMAL', ?, 'DAY', ?, ?)
				RETURNING chore_id
				""", Integer.class, familyId, parentId, childId, rewardPoints, today, today);
	}

	/**
	 * 대기 중인 완료 요청을 직접 넣는다. 승인·거절·취소 경합을 재현할 때 쓴다.
	 *
	 * <p>부분 유니크 인덱스 때문에 같은 집안일에 두 번 부르면 실패한다. 그게 의도다 —
	 * 대기 중 요청은 할 일당 하나뿐이다.
	 */
	public static Integer insertPendingRequest(JdbcTemplate jdbcTemplate, Integer choreId, Integer childId) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO chore_completion_requests (chore_id, child_id, description, status)
				VALUES (?, ?, '다 했어요', 'PENDING')
				RETURNING chore_completion_request_id
				""", Integer.class, choreId, childId);
	}

	public static String selectRequestStatus(JdbcTemplate jdbcTemplate, Integer requestId) {
		return jdbcTemplate.queryForObject(
				"SELECT status FROM chore_completion_requests WHERE chore_completion_request_id = ?",
				String.class, requestId);
	}

	public static int countRequestsByStatus(JdbcTemplate jdbcTemplate, Integer choreId, String status) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM chore_completion_requests WHERE chore_id = ? AND status = ?",
				Integer.class, choreId, status);
		return count == null ? 0 : count;
	}

	/** 그 아이에게 지급된 집안일 보상 원장 행 수. 다른 사유(충전·지급)는 세지 않는다. */
	public static int countRewardTransactions(JdbcTemplate jdbcTemplate, Integer childId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT count(*) FROM point_transactions
				WHERE child_id = ? AND transaction_type = 'CHORE_REWARD'
				""", Integer.class, childId);
		return count == null ? 0 : count;
	}
}
