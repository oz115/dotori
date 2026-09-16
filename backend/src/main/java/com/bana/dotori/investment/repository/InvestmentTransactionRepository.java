package com.bana.dotori.investment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.investment.entity.InvestmentTransaction;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, Integer> {

	List<InvestmentTransaction> findAllByFamilyIdAndChildId(Integer familyId, Integer childId);
}
