package com.bana.dotori.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FindIdRequest(
	@NotBlank String name,
	@NotBlank @Email String email
) {
}
