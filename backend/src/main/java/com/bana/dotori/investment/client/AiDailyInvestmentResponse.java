package com.bana.dotori.investment.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 POST /internal/investment/daily 응답 중 백엔드가 쓰는 부분만 매핑한다
 * (뉴스 목록도 내려오지만 로그 외 쓸 곳이 없어 받지 않는다).
 *
 * @param priceUpdatesWritten 오늘 새로 쓴 가격 이력 행 수 — 이미 처리된 날 재호출이면 0
 */
public record AiDailyInvestmentResponse(
	@JsonProperty("price_updates_written") int priceUpdatesWritten
) {
}
