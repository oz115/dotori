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

/**
 * 금액의 최소·단위 검증은 DTO 가 아니라 서비스(toPoints)가 전담한다 —
 * 여기서는 필수 여부와 name 규칙만 본다 (FamilyServiceImplTest 가 금액 정책을 검증).
 */
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
	@DisplayName("name의 앞뒤 공백은 수정 요청과 같은 규칙으로 잘려 저장된다")
	void validate_trimsName() {
		FamilyCreateRequest request = request("  도토리네 가족  ");

		assertThat(request.name()).isEqualTo("도토리네 가족");
		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("name이 공백뿐이면 위반이 발생한다")
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
	@DisplayName("추가 보상 한도가 null이면 위반이 발생한다")
	void validate_nullWeeklyRewardLimit() {
		FamilyCreateRequest request = new FamilyCreateRequest("도토리네 가족", 10000, null,
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
