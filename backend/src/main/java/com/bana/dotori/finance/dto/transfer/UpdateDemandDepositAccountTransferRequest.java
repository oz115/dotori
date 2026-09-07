package com.bana.dotori.finance.dto.transfer;

import com.bana.dotori.finance.dto.common.FinanceCommonHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record UpdateDemandDepositAccountTransferRequest(
	@JsonProperty("Header") FinanceCommonHeader header,
	@JsonProperty("depositAccountNo") String depositAccountNo,
	@JsonProperty("depositTransactionSummary") String depositTransactionSummary,
	@JsonProperty("transactionBalance") String transactionBalance,
	@JsonProperty("withdrawalAccountNo") String withdrawalAccountNo,
	@JsonProperty("withdrawalTransactionSummary") String withdrawalTransactionSummary
) {
}
