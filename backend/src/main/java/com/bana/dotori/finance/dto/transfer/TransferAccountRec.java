package com.bana.dotori.finance.dto.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record TransferAccountRec(
	@JsonProperty("transactionUniqueNo") String transactionUniqueNo,
	@JsonProperty("accountNo") String accountNo,
	@JsonProperty("transactionDate") String transactionDate,
	@JsonProperty("transactionType") String transactionType,
	@JsonProperty("transactionTypeName") String transactionTypeName,
	@JsonProperty("transactionAccountNo") String transactionAccountNo,
	@JsonProperty("transactionBalance") String transactionBalance
) {
}
