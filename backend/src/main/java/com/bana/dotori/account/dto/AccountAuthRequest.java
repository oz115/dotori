package com.bana.dotori.account.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountAuthRequest(
	@NotBlank String bankCode,
	@NotBlank String bankName,
	@NotBlank String accountNo
) {
}
