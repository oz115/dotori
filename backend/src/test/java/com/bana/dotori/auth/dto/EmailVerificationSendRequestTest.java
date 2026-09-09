package com.bana.dotori.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class EmailVerificationSendRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(EmailVerificationSendRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("이메일과 용도가 유효하면 위반이 없다")
	void validate_success() {
		assertThat(violatedFields(
			new EmailVerificationSendRequest("dotori@example.com", EmailVerificationPurpose.SIGNUP))).isEmpty();
	}

	@Test
	@DisplayName("용도는 선택이라 없어도 위반이 없다 (아이디 찾기·비밀번호 재설정 등 기존 호출 호환)")
	void validate_purposeIsOptional() {
		assertThat(violatedFields(new EmailVerificationSendRequest("dotori@example.com", null))).isEmpty();
	}

	@Test
	@DisplayName("이메일이 비어 있으면 위반이 발생한다")
	void validate_blankEmail() {
		assertThat(violatedFields(new EmailVerificationSendRequest(" ", null))).contains("email");
	}

	@Test
	@DisplayName("이메일 형식이 올바르지 않으면 위반이 발생한다")
	void validate_invalidEmail() {
		assertThat(violatedFields(new EmailVerificationSendRequest("not-an-email", null))).contains("email");
	}
}
