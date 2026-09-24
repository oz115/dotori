package com.bana.dotori.auth.dto;

public record EmailVerificationConfirmResponse(
	boolean verified,
	String verificationToken
) {
}
