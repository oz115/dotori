package com.bana.dotori.account.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountAuthVerifyRequest(
	@NotBlank String bankCode,
	@NotBlank String bankName,
	@NotBlank String accountNo,
	@NotBlank String authCode
) {
}
