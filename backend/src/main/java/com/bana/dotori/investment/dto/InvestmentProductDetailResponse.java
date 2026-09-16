package com.bana.dotori.investment.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.investment.entity.InvestmentProductCategory;

/**
 * 투자 상품 상세 — 목록(InvestmentProductSummary)에 설명·전일 가격·가격 갱신 일시가 더해진다.
 * 가격·변동은 조회한 사용자의 가족 기준이고, 등락은 %가 아니라 전일 대비 포인트 차이다.
 */
public record InvestmentProductDetailResponse(
	Integer investmentProductId,
	String name,
	String description,
	InvestmentProductCategory category,
	Integer currentPrice,
	Integer changeAmount,
	Integer previousPrice,
	Boolean isActive,
	OffsetDateTime updatedAt
) {
}
