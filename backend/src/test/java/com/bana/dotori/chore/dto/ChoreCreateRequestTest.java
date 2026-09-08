package com.bana.dotori.chore.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.chore.entity.ChoreRepeatType;

import jakarta.validation.ConstraintViolation;
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

	private ChoreCreateRequest request(Integer childId, String title, String description, Integer rewardPoints,
			ChoreRepeatType repeatType, LocalDate startDate, LocalDate endDate) {
		return new ChoreCreateRequest(childId, title, description, rewardPoints, repeatType, startDate, endDate);
	}

	private ChoreCreateRequest valid() {
		return request(2, "설거지", "저녁 먹고 나서", 500, ChoreRepeatType.DAY,
			LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
	}

	@Test
	@DisplayName("모든 값이 유효하면 위반이 없다")
	void validate_success() {
		Set<ConstraintViolation<ChoreCreateRequest>> violations = validator.validate(valid());

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("기간을 비워도 위반이 없다")
	void validate_nullPeriod() {
		ChoreCreateRequest request = request(2, "설거지", null, 500, ChoreRepeatType.DAY, null, null);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("childId가 null이면 위반이 발생한다")
	void validate_nullChildId() {
		ChoreCreateRequest request = request(null, "설거지", null, 500, ChoreRepeatType.DAY, null, null);

		assertThat(violatedFields(request)).contains("childId");
	}

	@Test
	@DisplayName("title이 비어 있으면 위반이 발생한다")
	void validate_blankTitle() {
		ChoreCreateRequest request = request(2, " ", null, 500, ChoreRepeatType.DAY, null, null);

		assertThat(violatedFields(request)).contains("title");
	}

	@Test
	@DisplayName("title이 100자를 초과하면 위반이 발생한다")
	void validate_tooLongTitle() {
		ChoreCreateRequest request = request(2, "가".repeat(101), null, 500, ChoreRepeatType.DAY, null, null);

		assertThat(violatedFields(request)).contains("title");
	}

	@Test
	@DisplayName("description이 255자를 초과하면 위반이 발생한다")
	void validate_tooLongDescription() {
		ChoreCreateRequest request = request(2, "설거지", "가".repeat(256), 500, ChoreRepeatType.DAY, null, null);

		assertThat(violatedFields(request)).contains("description");
	}

	@Test
	@DisplayName("rewardPoints가 0 이하면 위반이 발생한다")
	void validate_nonPositiveRewardPoints() {
		// 0 을 허용하면 "해도 아무것도 안 주는" 집안일이 만들어진다.
		assertThat(violatedFields(request(2, "설거지", null, 0, ChoreRepeatType.DAY, null, null)))
			.contains("rewardPoints");
		assertThat(violatedFields(request(2, "설거지", null, -1, ChoreRepeatType.DAY, null, null)))
			.contains("rewardPoints");
	}

	@Test
	@DisplayName("repeatType이 null이면 위반이 발생한다")
	void validate_nullRepeatType() {
		ChoreCreateRequest request = request(2, "설거지", null, 500, null, null, null);

		assertThat(violatedFields(request)).contains("repeatType");
	}

	@Test
	@DisplayName("종료일이 시작일보다 앞서면 위반이 발생한다")
	void validate_endBeforeStart() {
		ChoreCreateRequest request = request(2, "설거지", null, 500, ChoreRepeatType.DAY,
			LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 9));

		assertThat(violatedFields(request)).contains("periodOrdered");
	}

	@Test
	@DisplayName("종료일과 시작일이 같으면 위반이 없다")
	void validate_sameDay() {
		ChoreCreateRequest request = request(2, "설거지", null, 500, ChoreRepeatType.DAY,
			LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10));

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("시작일을 비운 채 종료일을 과거로 주면 위반이 발생한다")
	void validate_pastEndWithoutStart() {
		// startDate 를 비우면 오늘부터 시작하므로, 어제 끝나는 집안일은 성립하지 않는다.
		ChoreCreateRequest request = request(2, "설거지", null, 500, ChoreRepeatType.DAY,
			null, LocalDate.now().minusDays(1));

		assertThat(violatedFields(request)).contains("periodOrdered");
	}
}
