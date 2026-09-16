package com.bana.dotori.investment.dto;

/**
 * 보유 종목 한 건 — 수량·매수 가격은 investment_holdings 행 그대로, 현재가는 가족별 가격 이력 기준.
 * buyPrice 는 샀을 때 그 시점의 종목 가격이다 (종목당 1주 규칙이라 매수는 한 번뿐).
 * 프론트가 (currentPrice - buyPrice) x quantity 로 수익을 계산한다.
 */
public record InvestmentHoldingSummary(
	Integer investmentProductId,
	String productName,
	Integer quantity,
	Integer buyPrice,
	Integer currentPrice
) {
}
