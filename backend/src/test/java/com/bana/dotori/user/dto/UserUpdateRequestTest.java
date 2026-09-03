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

class UserUpdateRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(UserUpdateRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("이름이 유효하면 위반이 없다")
	void validate_success() {
		Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(new UserUpdateRequest("김도토리"));

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("이름을 아예 보내지 않으면(null) 부분 수정이므로 위반이 없다")
	void validate_absentNameIsAllowed() {
		Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(new UserUpdateRequest(null));

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("이름이 빈 문자열이면 위반이 발생한다")
	void validate_emptyName() {
		assertThat(violatedFields(new UserUpdateRequest(""))).contains("name");
	}

	@Test
	@DisplayName("이름이 공백뿐이면 위반이 발생한다")
	void validate_blankName() {
		assertThat(violatedFields(new UserUpdateRequest("   "))).contains("name");
	}

	@Test
	@DisplayName("이름이 50자를 초과하면 위반이 발생한다")
	void validate_tooLongName() {
		assertThat(violatedFields(new UserUpdateRequest("가".repeat(51)))).contains("name");
	}

	@Test
	@DisplayName("이름 앞뒤 공백은 잘라내고 보관한다")
	void trimsSurroundingWhitespace() {
		assertThat(new UserUpdateRequest("  김도토리  ").name()).isEqualTo("김도토리");
	}
}
