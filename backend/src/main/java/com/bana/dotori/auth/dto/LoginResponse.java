package com.bana.dotori.auth.dto;

import com.bana.dotori.user.entity.UserRole;

public record LoginResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	long expiresIn,
	UserSummary user
) {

	public record UserSummary(
		Integer userId,
		String name,
		UserRole role
	) {
	}
}
