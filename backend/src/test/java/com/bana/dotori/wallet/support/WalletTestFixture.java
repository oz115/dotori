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
	 * 자동 테스트가 만든 회원을 가려내는 표식. 여기서 네이티브 SQL 로 넣는 회원도,
	 * 마이페이지 E2E 가 회원가입 API 로 만드는 회원도 이메일을 {@code {loginId}@autotest.local} 로 맞춘다.
	 * 뒷정리와 집계는 전부 이 범위 안에서만 이루어진다.
	 *
	 * <p>{@code @dotori.test} 를 쓰다가 옮겼다. 팀이 개발용으로 넣어 둔 시드 회원이 같은 도메인을
	 * 쓰고 있어서, 테스트를 돌릴 때마다 그 시드가 지워지려 했다. 시드 가족을 참조하는
	 * store_products 가 삭제를 막아 FK 위반으로 지갑 테스트가 통째로 깨지기도 했다.
	 * 표식은 자동 테스트만 쓰는 값이어야 한다.
	 */
	private static final String TEST_USER_IDS = "SELECT user_id FROM users WHERE email LIKE '%@autotest.local'";

	/**
	 * fixture 가 만드는 회원의 비밀번호 원문. 로그인까지 태워야 하는 테스트가 쓴다.
	 */
	public static final String RAW_PASSWORD = "TestP@ss1";

	/**
	 * {@link #RAW_PASSWORD} 의 BCrypt 해시(strength 10). 애플리케이션이 저장하는 것과 같은 형식이라,
	 * 이 회원으로도 로그인이 된다.
	 *
	 * <p>예전에는 여기에 {@code 'test-encoded-password'} 라는 글자를 그대로 넣었다. 로그인은 어차피
	 * 안 되는 값이라 문제는 없었지만, 테스트가 도는 동안 공유 DB 를 열어 보면 password 컬럼에 해시가
	 * 아닌 값이 든 행으로 보여서 평문 저장으로 오해를 샀다.
	 */
	public static final String ENCODED_PASSWORD = "$2a$10$S2Lp1at18Xstot5/3YlWj.fq7U3/t3Qg18sfQoMEKjiAgwGcigdm2";

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
				VALUES (?, ?, ?, ?, ?)
				RETURNING user_id
				""", Integer.class, loginId, ENCODED_PASSWORD, loginId, loginId + "@autotest.local", role);
	}

	/**
	 * 가족을 만들고 그 안에 자녀를 넣는다. 지급(WLT-02)의 가족 소속 검증에 필요하다.
	 *
	 * <p>온보딩 설정 3종도 함께 넣는다 (S15P21A505-134 이후 실제 가족은 항상 이 값을 갖고
	 * 만들어지고, 최종 DDL 에서 NOT NULL 이라 빼면 테이블 재생성 시점에 INSERT 가 깨진다).
	 * 금액 단위는 저장 규칙대로 포인트다 (1000pt = 10,000원).
	 *
	 * @return 생성된 familyId
	 */
	public static Integer insertFamilyWithChild(JdbcTemplate jdbcTemplate, Integer parentId, Integer childId) {
		Integer familyId = jdbcTemplate.queryForObject("""
				INSERT INTO families (name, created_by, weekly_allowance, weekly_reward_limit,
					financial_knowledge_level)
				VALUES ('테스트 가족', ?, 1000, 500, 'BEGINNER') RETURNING family_id
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
	 * <p>테스트가 만든 회원({@code @autotest.local})과 거기 딸린 행만 지운다. 테이블을 통째로
	 * 비우지 않는 이유는, 이 DB 를 팀 전체가 공유하기 때문이다. 예전에는 {@code DELETE FROM users}
	 * 로 싹 지웠는데, 그러면 users 를 참조하는 테이블을 전부 먼저 비워야 해서 남의 도메인 데이터
	 * (연동 계좌 등)까지 딸려 나갔고, 다른 도메인이 users 참조 테이블을 추가할 때마다 삭제 목록이
	 * 낡아 FK 위반(23503)으로 지갑 테스트가 통째로 깨졌다.
	 *
	 * <p>이 메서드는 트랜잭션 없이 한 줄씩 커밋되므로 중간에 실패하면 앞의 DELETE 만 반영된 채
	 * 남는다. 범위를 좁혀 둔 덕에 그 피해도 테스트 데이터 안에서 끝난다.
	 */
	public static void cleanUp(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.update("DELETE FROM point_transactions WHERE child_id IN (" + TEST_USER_IDS + ")");
		jdbcTemplate.update("DELETE FROM point_wallets WHERE child_id IN (" + TEST_USER_IDS + ")");
		// 완료 요청이 chores 를 FK 로 참조하므로 chores 보다 먼저 지운다.
		// chores 는 parent_id·child_id 로 users 를 참조하므로, 둘 다 맨 아래 users 삭제보다 앞서야 한다.
		// 집안일 테이블을 빼 두면 이 목록이 낡아 FK 위반(23503)으로 뒷정리가 통째로 깨진다.
		jdbcTemplate.update("DELETE FROM chore_completion_requests WHERE child_id IN (" + TEST_USER_IDS + ")"
				+ " OR chore_id IN (SELECT chore_id FROM chores WHERE parent_id IN (" + TEST_USER_IDS + ")"
				+ " OR child_id IN (" + TEST_USER_IDS + "))");
		jdbcTemplate.update("DELETE FROM chores WHERE parent_id IN (" + TEST_USER_IDS + ")"
				+ " OR child_id IN (" + TEST_USER_IDS + ")"
				+ " OR family_id IN (SELECT family_id FROM families WHERE created_by IN (" + TEST_USER_IDS + "))");
		// purchases 가 store_products 를 FK 로 참조하므로, 아래 store_products 삭제보다 먼저 지운다.
		// 안 그러면 구매 이력이 남아 있는 상품을 지우려다 FK 위반(23503)으로 뒷정리 자체가 깨진다.
		jdbcTemplate.update("DELETE FROM purchases WHERE child_id IN (" + TEST_USER_IDS + ")"
				+ " OR family_id IN (SELECT family_id FROM families WHERE created_by IN (" + TEST_USER_IDS + "))");
		jdbcTemplate.update("DELETE FROM store_products WHERE created_by IN (" + TEST_USER_IDS + ")"
				+ " OR family_id IN (SELECT family_id FROM families WHERE created_by IN (" + TEST_USER_IDS + "))");
		jdbcTemplate.update("DELETE FROM family_codes WHERE family_id IN ("
				+ "SELECT family_id FROM families WHERE created_by IN (" + TEST_USER_IDS + "))");
		jdbcTemplate.update("DELETE FROM family_members WHERE user_id IN (" + TEST_USER_IDS + ")"
				+ " OR family_id IN (SELECT family_id FROM families WHERE created_by IN (" + TEST_USER_IDS + "))");
		jdbcTemplate.update("DELETE FROM families WHERE created_by IN (" + TEST_USER_IDS + ")");
		jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id IN (" + TEST_USER_IDS + ")");
		jdbcTemplate.update("DELETE FROM users WHERE email LIKE '%@autotest.local'");
	}

	public static int selectBalance(JdbcTemplate jdbcTemplate, Integer childId) {
		Integer balance = jdbcTemplate.queryForObject(
				"SELECT balance FROM point_wallets WHERE child_id = ?", Integer.class, childId);
		return balance == null ? 0 : balance;
	}

	/**
	 * 테스트가 만든 회원의 원장 행 수. 테이블 전체를 세면 공유 DB 에 남아 있는 남의 행까지
	 * 잡히므로 범위를 좁힌다. {@link #cleanUp(JdbcTemplate)} 과 같은 범위여야 한다.
	 */
	public static int countTransactions(JdbcTemplate jdbcTemplate) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM point_transactions WHERE child_id IN (" + TEST_USER_IDS + ")", Integer.class);
		return count == null ? 0 : count;
	}

	/**
	 * 테스트가 만든 회원의 원장에서 컬럼 하나를 읽는다. 행이 정확히 하나일 때만 쓴다.
	 */
	public static String selectOnlyTransactionColumn(JdbcTemplate jdbcTemplate, Integer childId, String column) {
		return jdbcTemplate.queryForObject(
				"SELECT " + column + " FROM point_transactions WHERE child_id = ?", String.class, childId);
	}
}
