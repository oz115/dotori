package com.bana.dotori.family.dto;

import java.time.OffsetDateTime;

public record InvitationCodeResponse(
	Integer familyId,
	String code,
	OffsetDateTime expiresAt
) {
}
