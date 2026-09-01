package com.bana.dotori.user.dto;

import com.bana.dotori.user.entity.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
	@NotBlank String loginId,
	@NotBlank String password,
	@NotBlank String name,
	@NotBlank @Email String email,
	@NotNull UserRole role
) {
}
