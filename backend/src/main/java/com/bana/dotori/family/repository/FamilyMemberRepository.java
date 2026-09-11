package com.bana.dotori.family.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.user.entity.UserRole;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Integer> {

	/**
	 * 이미 어느 가족에든 속해 있는지 확인한다 (FAM-01·초대 수락).
	 *
	 * <p>탈퇴한 회원의 소속은 세지 않는다. 남겨 두면 이미 없는 계정 때문에
	 * 가족 생성·연결이 막힌다.
	 */
	@Query("select count(fm) > 0 from FamilyMember fm "
		+ "where fm.user.id = :userId and fm.deletedAt is null and fm.user.deletedAt is null")
	boolean existsActiveMembership(@Param("userId") Integer userId);

	/**
	 * 사용자가 속한 살아 있는 가족의 id (가족 상태 조회).
	 *
	 * <p>초대 코드가 만료된 부모는 가족을 다시 만드는 게 아니라(ALREADY_IN_FAMILY 로 막힌다)
	 * 기존 가족에서 코드를 재발급해야 하는데, 그러려면 화면이 familyId 를 알아야 한다.
	 * 존재 여부(existsActiveMembership)와 같은 기준으로 걸러야 hasFamily 와 어긋나지 않는다.
	 * 활성 소속은 정책상 최대 1개다 — 생성·연결이 existsActiveMembership 으로 막는다.
	 */
	@Query("select fm.family.id from FamilyMember fm "
		+ "where fm.user.id = :userId and fm.deletedAt is null and fm.user.deletedAt is null")
	Optional<Integer> findActiveFamilyId(@Param("userId") Integer userId);

	/**
	 * 그 가족의 살아 있는 구성원인지 확인한다. 접근 권한 판정에 쓴다.
	 *
	 * <p>탈퇴한 회원은 구성원으로 보지 않는다.
	 */
	@Query("select count(fm) > 0 from FamilyMember fm "
		+ "where fm.family.id = :familyId and fm.user.id = :userId "
		+ "and fm.deletedAt is null and fm.user.deletedAt is null")
	boolean existsActiveMember(@Param("familyId") Integer familyId, @Param("userId") Integer userId);

	/**
	 * 가족 인원수 (FAM-02).
	 *
	 * <p>목록(findAllWithUserByFamilyId)과 같은 기준으로 세야 한다.
	 * 한쪽만 탈퇴 회원을 걸러 내면 "구성원 3명"인데 목록에는 2명만 나온다.
	 */
	@Query("select count(fm) from FamilyMember fm "
		+ "where fm.family.id = :familyId "
		+ "and fm.deletedAt is null and fm.user.deletedAt is null")
	int countActiveMembers(@Param("familyId") Integer familyId);

	/**
	 * 가족 구성원 목록 (FAM-03).
	 *
	 * <p>탈퇴한 회원(users.deleted_at)도 걸러야 한다. 소속만 보면 이미 계정을 지운 사람이
	 * 구성원으로 계속 보인다. 로그인은 이미 막히지만(AuthServiceImpl) 목록에는 남는다.
	 */
	@Query("select fm from FamilyMember fm join fetch fm.user "
		+ "where fm.family.id = :familyId "
		+ "and fm.deletedAt is null and fm.user.deletedAt is null "
		+ "order by fm.joinedAt")
	List<FamilyMember> findAllWithUserByFamilyId(@Param("familyId") Integer familyId);

	List<FamilyMember> findAllByFamilyIdAndDeletedAtIsNull(Integer familyId);

	/**
	 * 그 가족의 특정 구성원. 탈퇴한 회원은 찾지 않는다.
	 */
	@Query("select fm from FamilyMember fm join fetch fm.user "
		+ "where fm.family.id = :familyId and fm.user.id = :userId "
		+ "and fm.deletedAt is null and fm.user.deletedAt is null")
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
