package com.bana.dotori.family.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.family.entity.FamilyCode;

public interface FamilyCodeRepository extends JpaRepository<FamilyCode, Integer> {

	boolean existsByCode(String code);

	void deleteAllByFamilyId(Integer familyId);

	@Query("select fc from FamilyCode fc join fetch fc.family where fc.code = :code")
	Optional<FamilyCode> findWithFamilyByCode(@Param("code") String code);
}
