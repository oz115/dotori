package com.bana.dotori.finance.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CheckAuthCodeRec(
	@JsonProperty("status") String status,
	@JsonProperty("transactionUniqueNo") String transactionUniqueNo,
	@JsonProperty("accountNo") String accountNo
) {
}
