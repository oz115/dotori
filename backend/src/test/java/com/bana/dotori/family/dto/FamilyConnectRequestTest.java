package com.bana.dotori.family.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class FamilyConnectRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(FamilyConnectRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("code가 유효하면 위반이 없다")
	void validate_success() {
		FamilyConnectRequest request = new FamilyConnectRequest("A3B7K9");

		Set<ConstraintViolation<FamilyConnectRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("code가 비어 있으면 위반이 발생한다")
	void validate_blankCode() {
		FamilyConnectRequest request = new FamilyConnectRequest(" ");

		assertThat(violatedFields(request)).contains("code");
	}

	@Test
	@DisplayName("code가 null이면 위반이 발생한다")
	void validate_nullCode() {
		FamilyConnectRequest request = new FamilyConnectRequest(null);

		assertThat(violatedFields(request)).contains("code");
	}

	@Test
	@DisplayName("code가 100자를 초과하면 위반이 발생한다")
	void validate_tooLongCode() {
		FamilyConnectRequest request = new FamilyConnectRequest("A".repeat(101));

		assertThat(violatedFields(request)).contains("code");
	}
}
