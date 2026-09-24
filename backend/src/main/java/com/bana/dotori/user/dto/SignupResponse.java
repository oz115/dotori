package com.bana.dotori.user.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.user.entity.UserRole;

public record SignupResponse(
	Integer userId,
	String loginId,
	String name,
	String email,
	UserRole role,
	OffsetDateTime createdAt
) {
}
