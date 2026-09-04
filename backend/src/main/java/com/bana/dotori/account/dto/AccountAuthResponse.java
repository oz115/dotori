package com.bana.dotori.account.dto;

public record AccountAuthResponse(
	String accountNo,
	String authText,
	long expiresInSeconds
) {
}
