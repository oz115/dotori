package com.bana.dotori.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class LogoutRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	@DisplayName("refreshToken이 있으면 위반이 없다")
	void validate_success() {
		LogoutRequest request = new LogoutRequest("eyJhbGciOi...");

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("refreshToken이 비어 있으면 위반이 발생한다")
	void validate_blankRefreshToken() {
		LogoutRequest request = new LogoutRequest("");

		assertThat(validator.validate(request)).isNotEmpty();
	}
}
