package com.bana.dotori.family.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.user.entity.UserRole;

public record FamilyMemberResponse(
	Integer userId,
	String name,
	UserRole role,
	OffsetDateTime joinedAt
) {
}
