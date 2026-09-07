package com.bana.dotori.wallet.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 지갑 테스트가 필요로 하는 최소한의 데이터를 만든다.
 *
 * <p>users 는 회원가입 담당자가 아직 엔티티를 만드는 중이라 네이티브 SQL 로 넣는다.
 * 지금 User 엔티티를 임시로 만들어 두면 나중에 그 브랜치와 충돌하기 때문이다.
 * {@code users.role} 이 네이티브 ENUM 이라 명시적 캐스팅이 필요하다.
 */
public final class WalletTestFixture {

	/**
	 * 테스트가 만든 회원을 가려내는 표식. 여기서 네이티브 SQL 로 넣는 회원도,
	 * 마이페이지 E2E 가 회원가입 API 로 만드는 회원도 이메일을 {@code {loginId}@dotori.test} 로 맞춘다.
	 * 뒷정리와 집계는 전부 이 범위 안에서만 이루어진다.
	 */
	private static final String TEST_USER_IDS = "SELECT user_id FROM users WHERE email LIKE '%@dotori.test'";

	private WalletTestFixture() {
	}

	public static Integer insertChild(JdbcTemplate jdbcTemplate, String loginId) {
		return insertUser(jdbcTemplate, loginId, "CHILD");
	}

	public static Integer insertParent(JdbcTemplate jdbcTemplate, String loginId) {
		return insertUser(jdbcTemplate, loginId, "PARENT");
	}

	private static Integer insertUser(JdbcTemplate jdbcTemplate, String loginId, String role) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO users (login_id, password, name, email, role)
				VALUES (?, 'test-encoded-password', ?, ?, ?)
				RETURNING user_id
				""", Integer.class, loginId, loginId, loginId + "@dotori.test", role);
	}

	/**
	 * 가족을 만들고 그 안에 자녀를 넣는다. 지급(WLT-02)의 가족 소속 검증에 필요하다.
	 *
	 * @return 생성된 familyId
	 */
	public static Integer insertFamilyWithChild(JdbcTemplate jdbcTemplate, Integer parentId, Integer childId) {
		Integer familyId = jdbcTemplate.queryForObject("""
				INSERT INTO families (name, created_by) VALUES ('테스트 가족', ?) RETURNING family_id
				""", Integer.class, parentId);
		addFamilyMember(jdbcTemplate, familyId, parentId);
		addFamilyMember(jdbcTemplate, familyId, childId);
		return familyId;
	}

	public static void addFamilyMember(JdbcTemplate jdbcTemplate, Integer familyId, Integer userId) {
		jdbcTemplate.update("INSERT INTO family_members (family_id, user_id) VALUES (?, ?)", familyId, userId);
	}

	/**
	 * 트랜잭션 롤백에 기대지 않는 테스트(동시성 등)가 직접 뒷정리할 때 쓴다.
	 *
	 * <p>테스트가 만든 회원({@code @dotori.test})과 거기 딸린 행만 지운다. 테이블을 통째로
	 * 비우지 않는 이유는, 이 DB 를 팀 전체가 공유하기 때문이다. 예전에는 {@code DELETE FROM users}
	 * 로 싹 지웠는데, 그러면 users 를 참조하는 테이블을 전부 먼저 비워야 해서 남의 도메인 데이터
	 * (연동 계좌 등)까지 딸려 나갔고, 다른 도메인이 users 참조 테이블을 추가할 때마다 삭제 목록이
	 * 낡아 FK 위반(23503)으로 지갑 테스트가 통째로 깨졌다.
	 *
	 * <p>이 메서드는 트랜잭션 없이 한 줄씩 커밋되므로 중간에 실패하면 앞의 DELETE 만 반영된 채
	 * 남는다. 범위를 좁혀 둔 덕에 그 피해도 테스트 데이터 안에서 끝난다.
	 */
	public static void cleanUp(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.update("DELETE FROM point_transactions WHERE wallet_id IN ("
				+ "SELECT wallet_id FROM point_wallets WHERE user_id IN (" + TEST_USER_IDS + "))");
		jdbcTemplate.update("DELETE FROM point_wallets WHERE user_id IN (" + TEST_USER_IDS + ")");
		jdbcTemplate.update("DELETE FROM family_codes WHERE family_id IN ("
				+ "SELECT family_id FROM families WHERE created_by IN (" + TEST_USER_IDS + "))");
		jdbcTemplate.update("DELETE FROM family_members WHERE user_id IN (" + TEST_USER_IDS + ")"
				+ " OR family_id IN (SELECT family_id FROM families WHERE created_by IN (" + TEST_USER_IDS + "))");
		jdbcTemplate.update("DELETE FROM families WHERE created_by IN (" + TEST_USER_IDS + ")");
		jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id IN (" + TEST_USER_IDS + ")");
		jdbcTemplate.update("DELETE FROM users WHERE email LIKE '%@dotori.test'");
	}

	public static int selectBalance(JdbcTemplate jdbcTemplate, Integer userId) {
		Integer balance = jdbcTemplate.queryForObject(
				"SELECT balance FROM point_wallets WHERE user_id = ?", Integer.class, userId);
		return balance == null ? 0 : balance;
	}

	/**
	 * 테스트가 만든 회원의 원장 행 수. 테이블 전체를 세면 공유 DB 에 남아 있는 남의 행까지
	 * 잡히므로 범위를 좁힌다. {@link #cleanUp(JdbcTemplate)} 과 같은 범위여야 한다.
	 */
	public static int countTransactions(JdbcTemplate jdbcTemplate) {
		Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM point_transactions WHERE wallet_id IN ("
				+ "SELECT wallet_id FROM point_wallets WHERE user_id IN (" + TEST_USER_IDS + "))", Integer.class);
		return count == null ? 0 : count;
	}

	/**
	 * 테스트가 만든 회원의 원장에서 컬럼 하나를 읽는다. 행이 정확히 하나일 때만 쓴다.
	 */
	public static String selectOnlyTransactionColumn(JdbcTemplate jdbcTemplate, Integer userId, String column) {
		return jdbcTemplate.queryForObject("SELECT " + column + " FROM point_transactions"
				+ " WHERE wallet_id = (SELECT wallet_id FROM point_wallets WHERE user_id = ?)",
				String.class, userId);
	}
}
