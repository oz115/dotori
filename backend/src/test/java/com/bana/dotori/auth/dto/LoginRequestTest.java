package com.bana.dotori.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class LoginRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(LoginRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 필드가 유효하면 위반이 없다")
	void validate_success() {
		LoginRequest request = new LoginRequest("dotori123", "P@ssw0rd!");

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("loginId가 비어 있으면 위반이 발생한다")
	void validate_blankLoginId() {
		LoginRequest request = new LoginRequest(" ", "P@ssw0rd!");

		assertThat(violatedFields(request)).contains("loginId");
	}

	@Test
	@DisplayName("password가 비어 있으면 위반이 발생한다")
	void validate_blankPassword() {
		LoginRequest request = new LoginRequest("dotori123", "");

		assertThat(violatedFields(request)).contains("password");
	}
}
