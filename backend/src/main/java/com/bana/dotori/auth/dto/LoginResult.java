package com.bana.dotori.auth.dto;

public record LoginResult(
	LoginResponse response,
	String refreshToken
) {
}
