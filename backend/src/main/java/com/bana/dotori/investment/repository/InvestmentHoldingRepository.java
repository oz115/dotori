package com.bana.dotori.investment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.investment.entity.InvestmentHolding;

public interface InvestmentHoldingRepository extends JpaRepository<InvestmentHolding, Integer> {

	List<InvestmentHolding> findAllByFamilyIdAndChildIdOrderByInvestmentProductIdAsc(Integer familyId,
		Integer childId);
}
