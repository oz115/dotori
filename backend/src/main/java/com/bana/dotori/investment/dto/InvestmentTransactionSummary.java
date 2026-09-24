package com.bana.dotori.investment.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.investment.entity.InvestmentTransactionType;

/** 거래 이력 한 건 — investment_transactions 행 그대로 (1주 규칙이라 quantity 는 항상 1). */
public record InvestmentTransactionSummary(
	Integer investmentTransactionId,
	String productName,
	InvestmentTransactionType transactionType,
	Integer quantity,
	Integer unitPrice,
	Integer totalAmount,
	OffsetDateTime createdAt,
	Integer buyPrice,
	Integer realizedProfit
) {
	public InvestmentTransactionSummary(Integer id, String name, InvestmentTransactionType type,
		Integer quantity, Integer unitPrice, Integer totalAmount, OffsetDateTime createdAt) {
		this(id, name, type, quantity, unitPrice, totalAmount, createdAt, null, null);
	}
}
