package com.bana.dotori.finance.dto.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record SearchMemberRequest(
	@JsonProperty("apiKey") String apiKey,
	@JsonProperty("userId") String userId
) {
}
