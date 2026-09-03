package com.bana.dotori.family.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.user.entity.UserRole;

/**
 * 3계층 - MYPAGE-06 의 자녀 조회 쿼리가 실제 스키마에서 맞게 걸러 내는지 검증한다.
 *
 * <p>이 쿼리는 소프트 삭제를 users·family_members·families 세 군데에서 봐야 한다.
 * 조건 하나만 빠져도 결과는 "그럴듯하게" 나오기 때문에 단위 테스트로는 잡히지 않는다.
 * 트랜잭션 롤백에 기대므로 DB 데이터를 남기지 않는다.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("연결된 자녀 조회 쿼리")
class FamilyMemberRepositoryTest {

	@Autowired
	private FamilyMemberRepository familyMemberRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("같은 가족의 자녀만 합류 순으로 나온다")
	void 같은_가족_자녀만_합류순으로_나온다() {
		Integer parentId = insertUser("mypage-parent", "PARENT");
		Integer firstChildId = insertUser("mypage-child-1", "CHILD");
		Integer secondChildId = insertUser("mypage-child-2", "CHILD");
		Integer familyId = insertFamily(parentId);
		insertMember(familyId, parentId, OffsetDateTime.now().minusDays(3));
		insertMember(familyId, secondChildId, OffsetDateTime.now().minusDays(1));
		insertMember(familyId, firstChildId, OffsetDateTime.now().minusDays(2));

		List<FamilyMember> children = familyMemberRepository.findConnectedChildren(parentId, UserRole.CHILD);

		// 부모 본인은 빠지고, 자녀만 합류가 이른 순으로 나온다.
		assertThat(children).extracting(member -> member.getUser().getId())
			.containsExactly(firstChildId, secondChildId);
	}

	@Test
	@DisplayName("다른 가족의 자녀는 나오지 않는다")
	void 다른_가족_자녀는_나오지_않는다() {
		Integer parentId = insertUser("mypage-parent-a", "PARENT");
		Integer myChildId = insertUser("mypage-child-a", "CHILD");
		Integer familyId = insertFamily(parentId);
		insertMember(familyId, parentId, OffsetDateTime.now());
		insertMember(familyId, myChildId, OffsetDateTime.now());

		Integer otherParentId = insertUser("mypage-parent-b", "PARENT");
		Integer otherChildId = insertUser("mypage-child-b", "CHILD");
		Integer otherFamilyId = insertFamily(otherParentId);
		insertMember(otherFamilyId, otherParentId, OffsetDateTime.now());
		insertMember(otherFamilyId, otherChildId, OffsetDateTime.now());

		List<FamilyMember> children = familyMemberRepository.findConnectedChildren(parentId, UserRole.CHILD);

		assertThat(children).extracting(member -> member.getUser().getId()).containsExactly(myChildId);
	}

	@Test
	@DisplayName("가족에서 빠진 자녀는 나오지 않는다")
	void 가족에서_빠진_자녀는_나오지_않는다() {
		Integer parentId = insertUser("mypage-parent-c", "PARENT");
		Integer childId = insertUser("mypage-child-c", "CHILD");
		Integer familyId = insertFamily(parentId);
		insertMember(familyId, parentId, OffsetDateTime.now());
		insertMember(familyId, childId, OffsetDateTime.now());
		jdbcTemplate.update("UPDATE family_members SET deleted_at = now() WHERE user_id = ?", childId);

		assertThat(familyMemberRepository.findConnectedChildren(parentId, UserRole.CHILD)).isEmpty();
	}

	@Test
	@DisplayName("탈퇴한 자녀는 나오지 않는다")
	void 탈퇴한_자녀는_나오지_않는다() {
		Integer parentId = insertUser("mypage-parent-d", "PARENT");
		Integer childId = insertUser("mypage-child-d", "CHILD");
		Integer familyId = insertFamily(parentId);
		insertMember(familyId, parentId, OffsetDateTime.now());
		insertMember(familyId, childId, OffsetDateTime.now());
		jdbcTemplate.update("UPDATE users SET deleted_at = now() WHERE user_id = ?", childId);

		assertThat(familyMemberRepository.findConnectedChildren(parentId, UserRole.CHILD)).isEmpty();
	}

	@Test
	@DisplayName("해체된 가족의 자녀는 나오지 않는다")
	void 해체된_가족의_자녀는_나오지_않는다() {
		Integer parentId = insertUser("mypage-parent-e", "PARENT");
		Integer childId = insertUser("mypage-child-e", "CHILD");
		Integer familyId = insertFamily(parentId);
		insertMember(familyId, parentId, OffsetDateTime.now());
		insertMember(familyId, childId, OffsetDateTime.now());
		jdbcTemplate.update("UPDATE families SET deleted_at = now() WHERE family_id = ?", familyId);

		assertThat(familyMemberRepository.findConnectedChildren(parentId, UserRole.CHILD)).isEmpty();
	}

	@Test
	@DisplayName("가족이 없는 부모는 빈 목록을 받는다")
	void 가족이_없는_부모는_빈_목록을_받는다() {
		Integer parentId = insertUser("mypage-parent-f", "PARENT");

		assertThat(familyMemberRepository.findConnectedChildren(parentId, UserRole.CHILD)).isEmpty();
	}

	private Integer insertUser(String loginId, String role) {
		return jdbcTemplate.queryForObject("""
			INSERT INTO users (login_id, password, name, email, role)
			VALUES (?, 'test-encoded-password', ?, ?, ?)
			RETURNING user_id
			""", Integer.class, loginId, loginId, loginId + "@dotori.test", role);
	}

	private Integer insertFamily(Integer createdBy) {
		return jdbcTemplate.queryForObject("""
			INSERT INTO families (name, created_by) VALUES ('테스트 가족', ?) RETURNING family_id
			""", Integer.class, createdBy);
	}

	/**
	 * joined_at 을 직접 넣는다. DEFAULT CURRENT_TIMESTAMP 는 트랜잭션 시작 시각이라
	 * 한 테스트 안에서 넣은 행들이 모두 같은 값을 가져 정렬 검증이 무의미해진다.
	 */
	private void insertMember(Integer familyId, Integer userId, OffsetDateTime joinedAt) {
		jdbcTemplate.update("INSERT INTO family_members (family_id, user_id, joined_at) VALUES (?, ?, ?)",
			familyId, userId, joinedAt);
	}
}
