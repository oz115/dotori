package com.bana.dotori.finance.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record FinanceResponseHeader(
	@JsonProperty("responseCode") String responseCode,
	@JsonProperty("responseMessage") String responseMessage,
	@JsonProperty("apiName") String apiName,
	@JsonProperty("transmissionDate") String transmissionDate,
	@JsonProperty("transmissionTime") String transmissionTime,
	@JsonProperty("institutionCode") String institutionCode,
	@JsonProperty("apiKey") String apiKey,
	@JsonProperty("apiServiceCode") String apiServiceCode,
	@JsonProperty("institutionTransactionUniqueNo") String institutionTransactionUniqueNo
) {

	public boolean isSuccess() {
		return "H0000".equals(responseCode);
	}
}
