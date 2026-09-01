package com.bana.dotori.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
	@NotBlank String refreshToken
) {
}
