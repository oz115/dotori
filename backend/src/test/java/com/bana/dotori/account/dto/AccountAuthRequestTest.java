package com.bana.dotori.account.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AccountAuthRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(AccountAuthRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 필드가 유효하면 위반이 없다")
	void validate_success() {
		AccountAuthRequest request = new AccountAuthRequest("003", "국민은행", "0011214764051239");

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("bankCode가 비어 있으면 위반이 발생한다")
	void validate_blankBankCode() {
		AccountAuthRequest request = new AccountAuthRequest(" ", "국민은행", "0011214764051239");

		assertThat(violatedFields(request)).contains("bankCode");
	}

	@Test
	@DisplayName("bankName이 비어 있으면 위반이 발생한다")
	void validate_blankBankName() {
		AccountAuthRequest request = new AccountAuthRequest("003", " ", "0011214764051239");

		assertThat(violatedFields(request)).contains("bankName");
	}

	@Test
	@DisplayName("accountNo가 비어 있으면 위반이 발생한다")
	void validate_blankAccountNo() {
		AccountAuthRequest request = new AccountAuthRequest("003", "국민은행", " ");

		assertThat(violatedFields(request)).contains("accountNo");
	}
}
