package com.bana.dotori.investment.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 POST /internal/investment/families/{familyId}/price-backfill 응답.
 *
 * @param priceUpdatesWritten 새로 쓴 가격 이력 행 수 — 이미 이력이 있는 가족이면 0
 */
public record AiPriceBackfillResponse(
	@JsonProperty("price_updates_written") int priceUpdatesWritten
) {
}
