package com.bana.dotori.finance.dto.auth;

import com.bana.dotori.finance.dto.common.FinanceCommonHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record OpenAccountAuthRequest(
	@JsonProperty("Header") FinanceCommonHeader header,
	@JsonProperty("accountNo") String accountNo,
	@JsonProperty("authText") String authText
) {
}
