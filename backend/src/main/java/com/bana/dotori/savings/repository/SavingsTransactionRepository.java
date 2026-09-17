package com.bana.dotori.savings.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.savings.entity.SavingsTransaction;
import com.bana.dotori.savings.entity.SavingsTransactionType;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Integer> {

	List<SavingsTransaction> findBySavingsSubscriptionIdOrderByCreatedAtDesc(Integer savingsSubscriptionId);

	/** 이번 주기에 납입이 있었는지. 납입 리마인더가 미납 건만 고르려고 쓴다 */
	boolean existsBySavingsSubscriptionIdAndTransactionTypeInAndCreatedAtGreaterThanEqual(
		Integer savingsSubscriptionId, Collection<SavingsTransactionType> transactionTypes,
		OffsetDateTime createdAt);
}
