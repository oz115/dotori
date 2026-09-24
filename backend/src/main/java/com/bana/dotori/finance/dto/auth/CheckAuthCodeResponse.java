package com.bana.dotori.finance.dto.auth;

import com.bana.dotori.finance.dto.common.FinanceResponseHeader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CheckAuthCodeResponse(
	@JsonProperty("Header") FinanceResponseHeader header,
	@JsonProperty("REC") CheckAuthCodeRec rec
) {
}
