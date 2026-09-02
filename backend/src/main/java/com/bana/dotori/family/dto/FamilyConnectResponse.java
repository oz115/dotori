package com.bana.dotori.family.dto;

import java.time.OffsetDateTime;

public record FamilyConnectResponse(
	Integer familyId,
	String familyName,
	OffsetDateTime joinedAt
) {
}
