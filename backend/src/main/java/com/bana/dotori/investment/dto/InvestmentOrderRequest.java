package com.bana.dotori.investment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 매수/매도 요청. 종목당 1주 규칙이라 수량 필드가 없다 —
 * 매수는 항상 1주, 매도는 들고 있는 1주 전량이다.
 */
public record InvestmentOrderRequest(
	@NotNull(message = "종목 ID는 필수입니다")
	Integer investmentProductId
) {
}
