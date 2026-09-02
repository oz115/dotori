package com.bana.dotori.finance.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record FinanceCommonHeader(
	@JsonProperty("apiName") String apiName,
	@JsonProperty("transmissionDate") String transmissionDate,
	@JsonProperty("transmissionTime") String transmissionTime,
	@JsonProperty("institutionCode") String institutionCode,
	@JsonProperty("fintechAppNo") String fintechAppNo,
	@JsonProperty("apiServiceCode") String apiServiceCode,
	@JsonProperty("institutionTransactionUniqueNo") String institutionTransactionUniqueNo,
	@JsonProperty("apiKey") String apiKey,
	@JsonProperty("userKey") String userKey
) {
}
