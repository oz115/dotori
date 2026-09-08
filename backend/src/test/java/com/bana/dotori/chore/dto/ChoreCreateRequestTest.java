package com.bana.dotori.chore.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ChoreCreateRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(ChoreCreateRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	private ChoreCreateRequest request(String title, String description, ChoreDifficulty difficulty,
			ChoreRepeatType repeatType) {
		return new ChoreCreateRequest(title, description, difficulty, repeatType);
	}

	@Test
	@DisplayName("네 값이 유효하면 위반이 없다")
	void validate_success() {
		assertThat(validator.validate(
			request("설거지", "저녁 먹고 나서", ChoreDifficulty.NORMAL, ChoreRepeatType.DAY))).isEmpty();
	}

	@Test
	@DisplayName("설명은 비워도 위반이 없다")
	void validate_nullDescription() {
		assertThat(validator.validate(
			request("설거지", null, ChoreDifficulty.NORMAL, ChoreRepeatType.DAY))).isEmpty();
	}

	@Test
	@DisplayName("title이 비어 있으면 위반이 발생한다")
	void validate_blankTitle() {
		assertThat(violatedFields(request(" ", null, ChoreDifficulty.NORMAL, ChoreRepeatType.DAY)))
			.contains("title");
	}

	@Test
	@DisplayName("title이 100자를 초과하면 위반이 발생한다")
	void validate_tooLongTitle() {
		assertThat(violatedFields(
			request("가".repeat(101), null, ChoreDifficulty.NORMAL, ChoreRepeatType.DAY))).contains("title");
	}

	@Test
	@DisplayName("description이 255자를 초과하면 위반이 발생한다")
	void validate_tooLongDescription() {
		assertThat(violatedFields(
			request("설거지", "가".repeat(256), ChoreDifficulty.NORMAL, ChoreRepeatType.DAY)))
			.contains("description");
	}

	@Test
	@DisplayName("난이도가 null이면 위반이 발생한다")
	void validate_nullDifficulty() {
		assertThat(violatedFields(request("설거지", null, null, ChoreRepeatType.DAY))).contains("difficulty");
	}

	@Test
	@DisplayName("반복 주기가 null이면 위반이 발생한다")
	void validate_nullRepeatType() {
		assertThat(violatedFields(request("설거지", null, ChoreDifficulty.NORMAL, null)))
			.contains("repeatType");
	}
}
