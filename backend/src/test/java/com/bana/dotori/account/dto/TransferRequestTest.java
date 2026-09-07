package com.bana.dotori.account.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class TransferRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(TransferRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 필드가 유효하면 위반이 없다")
	void validate_success() {
		TransferRequest request = new TransferRequest("0011234567890", 10000L, "용돈");

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("memo가 없어도 위반이 없다")
	void validate_nullMemo() {
		TransferRequest request = new TransferRequest("0011234567890", 10000L, null);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("toAccountNo가 없으면 위반이 발생한다")
	void validate_blankToAccountNo() {
		TransferRequest request = new TransferRequest(" ", 10000L, null);

		assertThat(violatedFields(request)).contains("toAccountNo");
	}

	@Test
	@DisplayName("amount가 없으면 위반이 발생한다")
	void validate_nullAmount() {
		TransferRequest request = new TransferRequest("0011234567890", null, null);

		assertThat(violatedFields(request)).contains("amount");
	}

	@Test
	@DisplayName("amount가 0 이하이면 위반이 발생한다")
	void validate_nonPositiveAmount() {
		TransferRequest request = new TransferRequest("0011234567890", 0L, null);

		assertThat(violatedFields(request)).contains("amount");
	}

	@Test
	@DisplayName("memo가 50자를 초과하면 위반이 발생한다")
	void validate_tooLongMemo() {
		TransferRequest request = new TransferRequest("0011234567890", 10000L, "a".repeat(51));

		assertThat(violatedFields(request)).contains("memo");
	}
}
