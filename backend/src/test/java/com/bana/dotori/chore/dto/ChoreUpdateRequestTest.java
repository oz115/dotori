package com.bana.dotori.chore.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.chore.entity.ChoreRepeatType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ChoreUpdateRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(ChoreUpdateRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	private ChoreUpdateRequest request(String title, String description, Integer rewardPoints,
			ChoreRepeatType repeatType, LocalDate startDate, LocalDate endDate) {
		return new ChoreUpdateRequest(title, description, null, rewardPoints, repeatType, startDate, endDate);
	}

	@Test
	@DisplayName("전부 비어 있어도 위반이 없다")
	void validate_allNull() {
		// PATCH 라서 모든 필드가 선택이다. @NotBlank 를 쓰면 이 케이스가 막힌다.
		assertThat(validator.validate(request(null, null, null, null, null, null))).isEmpty();
	}

	@Test
	@DisplayName("한 필드만 보내도 위반이 없다")
	void validate_partial() {
		assertThat(validator.validate(request(null, null, 700, null, null, null))).isEmpty();
	}

	@Test
	@DisplayName("모든 값을 채워도 위반이 없다")
	void validate_full() {
		assertThat(validator.validate(request("설거지", "저녁 먹고", 500, ChoreRepeatType.WEEK,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))).isEmpty();
	}

	@Test
	@DisplayName("title을 보냈는데 공백이면 위반이 발생한다")
	void validate_blankTitle() {
		// null 은 "안 바꿈"이지만 공백은 실수다.
		assertThat(violatedFields(request(" ", null, null, null, null, null))).contains("title");
	}

	@Test
	@DisplayName("title이 100자를 초과하면 위반이 발생한다")
	void validate_tooLongTitle() {
		assertThat(violatedFields(request("가".repeat(101), null, null, null, null, null))).contains("title");
	}

	@Test
	@DisplayName("description이 255자를 초과하면 위반이 발생한다")
	void validate_tooLongDescription() {
		assertThat(violatedFields(request(null, "가".repeat(256), null, null, null, null)))
			.contains("description");
	}

	@Test
	@DisplayName("rewardPoints가 0 이하면 위반이 발생한다")
	void validate_nonPositiveRewardPoints() {
		assertThat(violatedFields(request(null, null, 0, null, null, null))).contains("rewardPoints");
		assertThat(violatedFields(request(null, null, -1, null, null, null))).contains("rewardPoints");
	}
}
