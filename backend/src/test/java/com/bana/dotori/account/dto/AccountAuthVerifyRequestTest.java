package com.bana.dotori.account.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AccountAuthVerifyRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(AccountAuthVerifyRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 필드가 유효하면 위반이 없다")
	void validate_success() {
		AccountAuthVerifyRequest request =
			new AccountAuthVerifyRequest("003", "국민은행", "0011214764051239", "1234");

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("bankCode가 비어 있으면 위반이 발생한다")
	void validate_blankBankCode() {
		AccountAuthVerifyRequest request =
			new AccountAuthVerifyRequest(" ", "국민은행", "0011214764051239", "1234");

		assertThat(violatedFields(request)).contains("bankCode");
	}

	@Test
	@DisplayName("bankName이 비어 있으면 위반이 발생한다")
	void validate_blankBankName() {
		AccountAuthVerifyRequest request =
			new AccountAuthVerifyRequest("003", " ", "0011214764051239", "1234");

		assertThat(violatedFields(request)).contains("bankName");
	}

	@Test
	@DisplayName("accountNo가 비어 있으면 위반이 발생한다")
	void validate_blankAccountNo() {
		AccountAuthVerifyRequest request =
			new AccountAuthVerifyRequest("003", "국민은행", " ", "1234");

		assertThat(violatedFields(request)).contains("accountNo");
	}

	@Test
	@DisplayName("authCode가 비어 있으면 위반이 발생한다")
	void validate_blankAuthCode() {
		AccountAuthVerifyRequest request =
			new AccountAuthVerifyRequest("003", "국민은행", "0011214764051239", " ");

		assertThat(violatedFields(request)).contains("authCode");
	}
}
