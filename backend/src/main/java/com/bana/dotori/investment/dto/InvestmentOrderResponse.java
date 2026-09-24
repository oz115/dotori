package com.bana.dotori.investment.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.investment.entity.InvestmentTransactionType;

/**
 * 매수/매도 체결 결과 — 남긴 거래 행 + 거래 후 포인트 잔액.
 * 시장가 체결이라 unitPrice 는 체결 시점의 가족 기준 현재가이고,
 * 1주 규칙이라 quantity 는 항상 1, totalAmount = unitPrice 다.
 */
public record InvestmentOrderResponse(
	Integer investmentTransactionId,
	InvestmentTransactionType transactionType,
	Integer quantity,
	Integer unitPrice,
	Integer totalAmount,
	Integer pointBalanceAfter,
	OffsetDateTime createdAt,
	Integer buyPrice,
	Integer realizedProfit
) {
	public InvestmentOrderResponse(Integer id, InvestmentTransactionType type, Integer quantity,
		Integer unitPrice, Integer totalAmount, Integer balance, OffsetDateTime createdAt) {
		this(id, type, quantity, unitPrice, totalAmount, balance, createdAt, null, null);
	}
}
