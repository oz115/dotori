package com.bana.dotori.investment.dto;

import com.bana.dotori.investment.entity.InvestmentProductCategory;

/** 투자 상품 목록의 한 건 — 현재가·등락률은 조회한 사용자의 가족 기준이다. */
public record InvestmentProductSummary(
	Integer investmentProductId,
	String name,
	InvestmentProductCategory category,
	Integer currentPrice,
	Double changeRate,
	Boolean isActive
) {
}
