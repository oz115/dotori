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
 * 투자 매수/매도 거래. 보유 수량·평균 매수 단가는 별도 테이블 없이
 * 이 거래들의 집계로 산출한다 (명세 INV-T-03 각주). 지금은 보유 조회가 읽기만 하고,
 * 행을 쓰는 것은 매수·매도 API 태스크의 몫이다.
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
