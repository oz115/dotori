package com.bana.dotori.finance.dto.transaction;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record InquireTransactionHistoryListRec(
	@JsonProperty("totalCount") String totalCount,
	@JsonProperty("list") List<TransactionHistoryItem> list
) {
}
