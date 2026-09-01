package com.bana.dotori.user.dto;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.user.entity.UserRole;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SignupRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(SignupRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 필드가 유효하면 위반이 없다")
	void validate_success() {
		SignupRequest request = new SignupRequest("dotori123", "P@ssw0rd!", "김도토", "dotori@example.com",
			UserRole.PARENT);

		Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("loginId가 비어 있으면 위반이 발생한다")
	void validate_blankLoginId() {
		SignupRequest request = new SignupRequest(" ", "P@ssw0rd!", "김도토", "dotori@example.com", UserRole.PARENT);

		assertThat(violatedFields(request)).contains("loginId");
	}

	@Test
	@DisplayName("password가 비어 있으면 위반이 발생한다")
	void validate_blankPassword() {
		SignupRequest request = new SignupRequest("dotori123", "", "김도토", "dotori@example.com", UserRole.PARENT);

		assertThat(violatedFields(request)).contains("password");
	}

	@Test
	@DisplayName("name이 비어 있으면 위반이 발생한다")
	void validate_blankName() {
		SignupRequest request = new SignupRequest("dotori123", "P@ssw0rd!", "", "dotori@example.com", UserRole.PARENT);

		assertThat(violatedFields(request)).contains("name");
	}

	@Test
	@DisplayName("email 형식이 올바르지 않으면 위반이 발생한다")
	void validate_invalidEmail() {
		SignupRequest request = new SignupRequest("dotori123", "P@ssw0rd!", "김도토", "not-an-email", UserRole.PARENT);

		assertThat(violatedFields(request)).contains("email");
	}

	@Test
	@DisplayName("role이 null이면 위반이 발생한다")
	void validate_nullRole() {
		SignupRequest request = new SignupRequest("dotori123", "P@ssw0rd!", "김도토", "dotori@example.com", null);

		assertThat(violatedFields(request)).contains("role");
	}
}
