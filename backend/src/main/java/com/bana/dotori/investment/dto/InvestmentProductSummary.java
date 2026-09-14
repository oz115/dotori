package com.bana.dotori.investment.dto;

import com.bana.dotori.investment.entity.InvestmentProductCategory;

/**
 * 투자 상품 목록의 한 건 — 현재가·변동 포인트는 조회한 사용자의 가족 기준이다.
 * 등락은 %가 아니라 전일 대비 포인트 차이로 내려 준다 (팀 결정: 아이에게 %는 어렵다).
 */
public record InvestmentProductSummary(
	Integer investmentProductId,
	String name,
	InvestmentProductCategory category,
	Integer currentPrice,
	Integer changeAmount,
	Boolean isActive
) {
}
