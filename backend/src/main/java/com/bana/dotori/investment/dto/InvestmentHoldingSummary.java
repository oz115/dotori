package com.bana.dotori.investment.dto;

/**
 * 보유 종목 한 건 — 수량·매수 단가는 investment_holdings 행 그대로, 현재가는 가족별 가격 이력 기준.
 * averageBuyPrice 는 명세 필드명이고, 종목당 1주 규칙에서는 그냥 "샀을 때 가격"이다.
 * 프론트가 (currentPrice - averageBuyPrice) x quantity 로 수익을 계산한다.
 */
public record InvestmentHoldingSummary(
	Integer investmentProductId,
	String productName,
	Integer quantity,
	Integer averageBuyPrice,
	Integer currentPrice
) {
}
