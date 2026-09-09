package com.bana.dotori.auth.dto;

public record TokenRefreshResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	long expiresIn
) {
}
