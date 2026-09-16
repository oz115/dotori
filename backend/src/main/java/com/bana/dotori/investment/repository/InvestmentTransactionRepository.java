package com.bana.dotori.investment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.investment.entity.InvestmentTransaction;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, Integer> {
}
