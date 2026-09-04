package com.bana.dotori.finance.dto.transaction;

import com.bana.dotori.finance.dto.common.FinanceCommonHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record InquireTransactionHistoryListRequest(
	@JsonProperty("Header") FinanceCommonHeader header,
	@JsonProperty("accountNo") String accountNo,
	@JsonProperty("startDate") String startDate,
	@JsonProperty("endDate") String endDate,
	@JsonProperty("transactionType") String transactionType,
	@JsonProperty("orderByType") String orderByType
) {
}
