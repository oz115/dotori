package com.bana.dotori.family.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.family.entity.FamilyMember;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Integer> {

	boolean existsByUserId(Integer userId);
}
