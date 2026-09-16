package com.bana.dotori.investment.dto;

import java.util.List;

/**
 * 종목 가격 이력 (상세 화면 차트용). 가격은 가족별이라 조회한 사용자의 가족 기준이고,
 * AI 가 아직 가격을 안 움직인 기간은 행이 없어 배열이 비거나 짧을 수 있다.
 */
public record InvestmentPriceHistoryResponse(
	Integer investmentProductId,
	List<InvestmentPricePoint> history
) {
}
