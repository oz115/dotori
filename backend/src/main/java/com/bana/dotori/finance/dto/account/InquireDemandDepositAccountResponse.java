package com.bana.dotori.finance.dto.account;

import com.bana.dotori.finance.dto.common.FinanceResponseHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record InquireDemandDepositAccountResponse(
	@JsonProperty("Header") FinanceResponseHeader header,
	@JsonProperty("REC") InquireDemandDepositAccountRec rec
) {
}
