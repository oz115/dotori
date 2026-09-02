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

class FamilyCreateRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(FamilyCreateRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("name이 유효하면 위반이 없다")
	void validate_success() {
		FamilyCreateRequest request = new FamilyCreateRequest("도토리네 가족");

		Set<ConstraintViolation<FamilyCreateRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("name이 비어 있으면 위반이 발생한다")
	void validate_blankName() {
		FamilyCreateRequest request = new FamilyCreateRequest(" ");

		assertThat(violatedFields(request)).contains("name");
	}

	@Test
	@DisplayName("name이 null이면 위반이 발생한다")
	void validate_nullName() {
		FamilyCreateRequest request = new FamilyCreateRequest(null);

		assertThat(violatedFields(request)).contains("name");
	}

	@Test
	@DisplayName("name이 100자를 초과하면 위반이 발생한다")
	void validate_tooLongName() {
		FamilyCreateRequest request = new FamilyCreateRequest("가".repeat(101));

		assertThat(violatedFields(request)).contains("name");
	}
}
