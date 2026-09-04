package com.bana.dotori.finance.dto.account;

import com.bana.dotori.finance.dto.common.FinanceCommonHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record InquireDemandDepositAccountRequest(
	@JsonProperty("Header") FinanceCommonHeader header,
	@JsonProperty("accountNo") String accountNo
) {
}
