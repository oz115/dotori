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
	 */
	public static void cleanUp(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.execute("DELETE FROM point_transactions");
		jdbcTemplate.execute("DELETE FROM point_wallets");
		jdbcTemplate.execute("DELETE FROM family_members");
		jdbcTemplate.execute("DELETE FROM families");
		jdbcTemplate.execute("DELETE FROM users");
	}

	public static int selectBalance(JdbcTemplate jdbcTemplate, Integer userId) {
		Integer balance = jdbcTemplate.queryForObject(
				"SELECT balance FROM point_wallets WHERE user_id = ?", Integer.class, userId);
		return balance == null ? 0 : balance;
	}

	public static int countTransactions(JdbcTemplate jdbcTemplate) {
		Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM point_transactions", Integer.class);
		return count == null ? 0 : count;
	}
}
