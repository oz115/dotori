package com.bana.dotori.family.dto;

import java.time.OffsetDateTime;

public record FamilyCreateResponse(
	Integer familyId,
	String name,
	Integer createdBy,
	OffsetDateTime createdAt
) {
}
