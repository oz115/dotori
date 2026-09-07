package com.bana.dotori.finance.dto.transfer;

import java.util.List;

import com.bana.dotori.finance.dto.common.FinanceResponseHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record UpdateDemandDepositAccountTransferResponse(
	@JsonProperty("Header") FinanceResponseHeader header,
	@JsonProperty("REC") List<TransferAccountRec> rec
) {
}
