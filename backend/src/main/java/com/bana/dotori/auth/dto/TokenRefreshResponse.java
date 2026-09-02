package com.bana.dotori.auth.dto;

public record TokenRefreshResponse(
	String accessToken,
	String tokenType,
	long expiresIn
) {
}
