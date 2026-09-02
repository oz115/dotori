package com.bana.dotori.family.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.family.entity.FamilyMember;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Integer> {

	boolean existsByUserId(Integer userId);

	boolean existsByFamilyIdAndUserId(Integer familyId, Integer userId);

	long countByFamilyId(Integer familyId);

	@Query("select fm from FamilyMember fm join fetch fm.user where fm.family.id = :familyId order by fm.joinedAt")
	List<FamilyMember> findAllWithUserByFamilyId(@Param("familyId") Integer familyId);

	void deleteAllByFamilyId(Integer familyId);
}
