package com.bana.dotori.investment.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투자 매수/매도 거래 이력. 회차별 매수 가격의 원본 기록이고,
 * 현재 보유 현황은 investment_holdings 가 따로 들고 있다 — 매수·매도 API 태스크가
 * 두 테이블을 같은 트랜잭션으로 쓴다 (지금 이 엔티티를 읽는 곳은 없다).
 */
@Entity
@Table(name = "investment_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "investment_transaction_id")
	private Integer id;

	@Column(name = "family_id", nullable = false)
	private Integer familyId;

	@Column(name = "child_id", nullable = false)
	private Integer childId;

	@Column(name = "investment_product_id", nullable = false)
	private Integer investmentProductId;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false)
	private InvestmentTransactionType transactionType;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "unit_price", nullable = false)
	private Integer unitPrice;

	@Column(name = "total_amount", nullable = false)
	private Integer totalAmount;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Builder
	private InvestmentTransaction(Integer familyId, Integer childId, Integer investmentProductId,
		InvestmentTransactionType transactionType, Integer quantity, Integer unitPrice, Integer totalAmount,
		OffsetDateTime createdAt) {
		this.familyId = familyId;
		this.childId = childId;
		this.investmentProductId = investmentProductId;
		this.transactionType = transactionType;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.totalAmount = totalAmount;
		this.createdAt = createdAt;
	}
}
