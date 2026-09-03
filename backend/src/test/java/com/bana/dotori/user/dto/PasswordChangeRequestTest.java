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

class PasswordChangeRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(PasswordChangeRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("두 값이 모두 있으면 위반이 없다")
	void validate_success() {
		Set<ConstraintViolation<PasswordChangeRequest>> violations =
			validator.validate(new PasswordChangeRequest("P@ssw0rd!", "NewP@ssw0rd!"));

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("currentPassword 가 없으면 위반이 발생한다")
	void validate_blankCurrentPassword() {
		assertThat(violatedFields(new PasswordChangeRequest(" ", "NewP@ssw0rd!"))).contains("currentPassword");
	}

	@Test
	@DisplayName("newPassword 는 여기서 막지 않는다 - 정책 위반은 서비스가 INVALID_PASSWORD_FORMAT 으로 낸다")
	void validate_newPasswordIsCheckedInService() {
		assertThat(violatedFields(new PasswordChangeRequest("P@ssw0rd!", "short"))).isEmpty();
		assertThat(violatedFields(new PasswordChangeRequest("P@ssw0rd!", null))).isEmpty();
	}
}
