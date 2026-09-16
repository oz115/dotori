package com.bana.dotori.savings.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가입 건 하나의 거래 이력 한 줄. 금액은 항상 양수이고 방향은 {@link SavingsTransactionType} 이 정한다
 * (DB 의 {@code CK_SAVINGS_TRANSACTIONS_AMOUNT_POSITIVE} 와 같은 약속이다).
 */
@Entity
@Table(name = "savings_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingsTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "savings_transaction_id")
	private Integer id;

	@Column(name = "savings_subscription_id", nullable = false, updatable = false)
	private Integer savingsSubscriptionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, updatable = false)
	private SavingsTransactionType transactionType;

	@Column(name = "amount", nullable = false, updatable = false)
	private int amount;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	private SavingsTransaction(Integer savingsSubscriptionId, SavingsTransactionType transactionType, int amount) {
		this.savingsSubscriptionId = savingsSubscriptionId;
		this.transactionType = transactionType;
		this.amount = amount;
	}

	public static SavingsTransaction record(Integer savingsSubscriptionId, SavingsTransactionType transactionType,
		int amount) {
		return new SavingsTransaction(savingsSubscriptionId, transactionType, amount);
	}
}
