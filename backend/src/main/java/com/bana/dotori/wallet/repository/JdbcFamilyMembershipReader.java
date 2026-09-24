package com.bana.dotori.wallet.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * family_members 를 직접 조회하는 임시 어댑터.
 *
 * <p>엔티티 대신 네이티브 SQL 을 쓰는 이유는 가족 도메인 담당자의 매핑과 충돌하지 않기 위해서다.
 * 읽기 한 건뿐이라 SQL 로 두는 비용이 작고, 나중에 가족 서비스로 갈아끼우기도 쉽다.
 */
@Repository
@RequiredArgsConstructor
public class JdbcFamilyMembershipReader implements FamilyMembershipReader {

	/**
	 * 소속 판정은 세 가지 소프트 삭제를 모두 봐야 한다.
	 * 탈퇴한 사용자(users), 가족에서 빠진 소속(family_members), 해체된 가족(families) 중
	 * 하나라도 놓치면 이미 관계가 끊긴 사람이 지갑을 건드릴 수 있다.
	 */
	private static final String IS_CHILD_OF_FAMILY = """
			SELECT EXISTS (
				SELECT 1
				FROM family_members fm
				JOIN users u ON u.user_id = fm.user_id
				JOIN families f ON f.family_id = fm.family_id
				WHERE fm.family_id = ?
				  AND fm.user_id = ?
				  AND u.role = 'CHILD'
				  AND u.deleted_at IS NULL
				  AND fm.deleted_at IS NULL
				  AND f.deleted_at IS NULL
			)
			""";

	private static final String IS_PARENT_OF_FAMILY = """
			SELECT EXISTS (
				SELECT 1
				FROM family_members fm
				JOIN users u ON u.user_id = fm.user_id
				JOIN families f ON f.family_id = fm.family_id
				WHERE fm.family_id = ?
				  AND fm.user_id = ?
				  AND u.role = 'PARENT'
				  AND u.deleted_at IS NULL
				  AND fm.deleted_at IS NULL
				  AND f.deleted_at IS NULL
			)
			""";

	/**
	 * 부모가 여럿이면 먼저 합류한 사람을 쓴다. 포인트 충전 시 돈을 받을 부모 계좌를
	 * 하나로 정해야 하는데, 가족 도메인에 "대표 부모" 개념이 없어 여기서 임시로 정한다.
	 */
	private static final String FIND_PARENT_USER_ID_OF_FAMILY = """
			SELECT fm.user_id
			FROM family_members fm
			JOIN users u ON u.user_id = fm.user_id
			JOIN families f ON f.family_id = fm.family_id
			WHERE fm.family_id = ?
			  AND u.role = 'PARENT'
			  AND u.deleted_at IS NULL
			  AND fm.deleted_at IS NULL
			  AND f.deleted_at IS NULL
			ORDER BY fm.joined_at
			""";

	private final JdbcTemplate jdbcTemplate;

	@Override
	public boolean isChildOfFamily(Integer familyId, Integer childId) {
		if (familyId == null || childId == null) {
			return false;
		}
		return Boolean.TRUE.equals(
				jdbcTemplate.queryForObject(IS_CHILD_OF_FAMILY, Boolean.class, familyId, childId));
	}

	@Override
	public boolean isParentOfFamily(Integer familyId, Integer parentId) {
		if (familyId == null || parentId == null) {
			return false;
		}
		return Boolean.TRUE.equals(
				jdbcTemplate.queryForObject(IS_PARENT_OF_FAMILY, Boolean.class, familyId, parentId));
	}

	@Override
	public Integer findParentUserId(Integer familyId) {
		if (familyId == null) {
			return null;
		}
		List<Integer> parentUserIds = jdbcTemplate.query(FIND_PARENT_USER_ID_OF_FAMILY,
				(rs, rowNum) -> rs.getInt("user_id"), familyId);
		return parentUserIds.isEmpty() ? null : parentUserIds.get(0);
	}
}
