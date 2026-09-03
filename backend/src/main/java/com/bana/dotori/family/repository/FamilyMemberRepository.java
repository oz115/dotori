package com.bana.dotori.family.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.user.entity.UserRole;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Integer> {

	boolean existsByUserIdAndDeletedAtIsNull(Integer userId);

	boolean existsByFamilyIdAndUserIdAndDeletedAtIsNull(Integer familyId, Integer userId);

	int countByFamilyIdAndDeletedAtIsNull(Integer familyId);

	@Query("select fm from FamilyMember fm join fetch fm.user "
		+ "where fm.family.id = :familyId and fm.deletedAt is null order by fm.joinedAt")
	List<FamilyMember> findAllWithUserByFamilyId(@Param("familyId") Integer familyId);

	List<FamilyMember> findAllByFamilyIdAndDeletedAtIsNull(Integer familyId);

	@Query("select fm from FamilyMember fm join fetch fm.user "
		+ "where fm.family.id = :familyId and fm.user.id = :userId and fm.deletedAt is null")
	Optional<FamilyMember> findWithUserByFamilyIdAndUserId(@Param("familyId") Integer familyId,
		@Param("userId") Integer userId);

	/**
	 * 부모가 속한 가족의 자녀 구성원을 합류 순으로 조회한다 (MYPAGE-06).
	 *
	 * <p>소프트 삭제를 세 군데 모두 봐야 한다. 탈퇴한 사용자(users), 가족에서 빠진
	 * 소속(family_members), 해체된 가족(families) 중 하나라도 놓치면 이미 관계가 끊긴
	 * 자녀가 부모의 마이페이지에 계속 보인다.
	 */
	@Query("select fm from FamilyMember fm join fetch fm.user "
		+ "where fm.family.id in ("
		+ "select pm.family.id from FamilyMember pm "
		+ "where pm.user.id = :parentUserId and pm.deletedAt is null) "
		+ "and fm.user.role = :childRole "
		+ "and fm.user.deletedAt is null "
		+ "and fm.deletedAt is null "
		+ "and fm.family.deletedAt is null "
		+ "order by fm.joinedAt")
	List<FamilyMember> findConnectedChildren(@Param("parentUserId") Integer parentUserId,
		@Param("childRole") UserRole childRole);
}
