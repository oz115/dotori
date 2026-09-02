package com.bana.dotori.family.dto;

import java.time.OffsetDateTime;

public record FamilyResponse(
	Integer familyId,
	String name,
	Integer createdBy,
	long memberCount,
	OffsetDateTime createdAt
) {
}
