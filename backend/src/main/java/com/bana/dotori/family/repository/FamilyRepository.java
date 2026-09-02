package com.bana.dotori.family.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.family.entity.Family;

public interface FamilyRepository extends JpaRepository<Family, Integer> {
}
