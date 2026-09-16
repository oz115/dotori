package com.bana.dotori.savings.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.savings.entity.SavingsTransaction;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Integer> {

	List<SavingsTransaction> findBySavingsSubscriptionIdOrderByCreatedAtDesc(Integer savingsSubscriptionId);
}
