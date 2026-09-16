package com.bana.dotori.investment.dto;

/** 보유 종목 한 건 — 수량·평균 매수 단가는 거래 집계, 현재가는 가족별 가격 이력 기준이다. */
public record InvestmentHoldingSummary(
	Integer investmentProductId,
	String productName,
	Integer quantity,
	Integer averageBuyPrice,
	Integer currentPrice
) {
}
