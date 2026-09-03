package com.bana.dotori.finance.dto.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record SearchMemberResponse(
	@JsonProperty("userId") String userId,
	@JsonProperty("userName") String userName,
	@JsonProperty("institutionCode") String institutionCode,
	@JsonProperty("userKey") String userKey,
	@JsonProperty("created") String created,
	@JsonProperty("modified") String modified
) {
}
