package com.bana.dotori.finance.dto.transaction;

import com.bana.dotori.finance.dto.common.FinanceResponseHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record InquireTransactionHistoryListResponse(
	@JsonProperty("Header") FinanceResponseHeader header,
	@JsonProperty("REC") InquireTransactionHistoryListRec rec
) {
}
