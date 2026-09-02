package com.bana.dotori.auth.dto;

public record TokenRefreshResult(
	TokenRefreshResponse response,
	String refreshToken
) {
}
