package com.bana.dotori.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferRequest(
	@NotBlank String toAccountNo,
	@NotNull @Positive Long amount,
	@Size(max = 50) String memo
) {
}
