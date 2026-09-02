package com.bana.dotori.family.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.family.entity.Family;

import jakarta.persistence.LockModeType;

public interface FamilyRepository extends JpaRepository<Family, Integer> {

	Optional<Family> findByIdAndDeletedAtIsNull(Integer familyId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select f from Family f where f.id = :familyId and f.deletedAt is null")
	Optional<Family> findByIdAndDeletedAtIsNullForUpdate(@Param("familyId") Integer familyId);
}
