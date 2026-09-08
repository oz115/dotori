package com.bana.dotori.user.dto;

import com.bana.dotori.global.util.PasswordPolicy;
import com.bana.dotori.user.entity.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
	@NotBlank @Size(max = 50) String loginId,
	@NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String password,
	@NotBlank @Size(max = 50) String name,
	@NotBlank @Email @Size(max = 255) String email,
	@NotNull UserRole role,
	/* 이메일 인증 코드 확인(AUTH-07)에서 발급받은 토큰. 인증된 이메일만 가입할 수 있다. */
	@NotBlank String verificationToken
) {
}
