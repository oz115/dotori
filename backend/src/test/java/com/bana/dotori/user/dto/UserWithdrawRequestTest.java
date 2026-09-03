package com.bana.dotori.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UserWithdrawRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	@DisplayName("비밀번호가 있으면 위반이 없다")
	void validate_success() {
		Set<ConstraintViolation<UserWithdrawRequest>> violations =
			validator.validate(new UserWithdrawRequest("P@ssw0rd!"));

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("비밀번호가 비어 있으면 위반이 발생한다")
	void validate_blankPassword() {
		Set<String> fields = validator.validate(new UserWithdrawRequest(" ")).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());

		assertThat(fields).contains("password");
	}
}
