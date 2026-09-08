package com.bana.dotori.family.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class FamilyCreateRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private FamilyCreateRequest request(String name) {
		return new FamilyCreateRequest(name, 10000, 5000, FinancialKnowledgeLevel.BEGINNER);
	}

	private Set<String> violatedFields(FamilyCreateRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 필드가 유효하면 위반이 없다")
	void validate_success() {
		Set<ConstraintViolation<FamilyCreateRequest>> violations = validator.validate(request("도토리네 가족"));

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("name이 비어 있으면 위반이 발생한다")
	void validate_blankName() {
		assertThat(violatedFields(request(" "))).contains("name");
	}

	@Test
	@DisplayName("name이 null이면 위반이 발생한다")
	void validate_nullName() {
		assertThat(violatedFields(request(null))).contains("name");
	}

	@Test
	@DisplayName("name이 100자를 초과하면 위반이 발생한다")
	void validate_tooLongName() {
		assertThat(violatedFields(request("가".repeat(101)))).contains("name");
	}

	@Test
	@DisplayName("기본 용돈이 null이면 위반이 발생한다")
	void validate_nullWeeklyAllowance() {
		FamilyCreateRequest request = new FamilyCreateRequest("도토리네 가족", null, 5000,
			FinancialKnowledgeLevel.BEGINNER);

		assertThat(violatedFields(request)).contains("weeklyAllowance");
	}

	@Test
	@DisplayName("기본 용돈이 음수면 위반이 발생한다")
	void validate_negativeWeeklyAllowance() {
		FamilyCreateRequest request = new FamilyCreateRequest("도토리네 가족", -1, 5000,
			FinancialKnowledgeLevel.BEGINNER);

		assertThat(violatedFields(request)).contains("weeklyAllowance");
	}

	@Test
	@DisplayName("추가 보상 한도가 null이면 위반이 발생한다")
	void validate_nullWeeklyRewardLimit() {
		FamilyCreateRequest request = new FamilyCreateRequest("도토리네 가족", 10000, null,
			FinancialKnowledgeLevel.BEGINNER);

		assertThat(violatedFields(request)).contains("weeklyRewardLimit");
	}

	@Test
	@DisplayName("추가 보상 한도가 음수면 위반이 발생한다")
	void validate_negativeWeeklyRewardLimit() {
		FamilyCreateRequest request = new FamilyCreateRequest("도토리네 가족", 10000, -1,
			FinancialKnowledgeLevel.BEGINNER);

		assertThat(violatedFields(request)).contains("weeklyRewardLimit");
	}

	@Test
	@DisplayName("금융 지식 수준이 null이면 위반이 발생한다")
	void validate_nullFinancialKnowledgeLevel() {
		FamilyCreateRequest request = new FamilyCreateRequest("도토리네 가족", 10000, 5000, null);

		assertThat(violatedFields(request)).contains("financialKnowledgeLevel");
	}
}
