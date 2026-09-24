package com.bana.dotori.finance.dto.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record TransactionHistoryItem(
	@JsonProperty("transactionUniqueNo") String transactionUniqueNo,
	@JsonProperty("transactionDate") String transactionDate,
	@JsonProperty("transactionTime") String transactionTime,
	@JsonProperty("transactionType") String transactionType,
	@JsonProperty("transactionTypeName") String transactionTypeName,
	@JsonProperty("transactionAccountNo") String transactionAccountNo,
	@JsonProperty("transactionBalance") String transactionBalance,
	@JsonProperty("transactionAfterBalance") String transactionAfterBalance,
	@JsonProperty("transactionSummary") String transactionSummary,
	@JsonProperty("transactionMemo") String transactionMemo
) {
}
