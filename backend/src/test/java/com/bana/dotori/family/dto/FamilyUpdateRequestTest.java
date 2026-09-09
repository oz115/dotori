package com.bana.dotori.family.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class FamilyUpdateRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(FamilyUpdateRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("부분 수정이라 모든 필드가 null 이어도 위반이 없다")
	void validate_allNull() {
		assertThat(violatedFields(new FamilyUpdateRequest(null, null, null, null))).isEmpty();
	}

	@Test
	@DisplayName("모든 필드가 유효하면 위반이 없다")
	void validate_success() {
		FamilyUpdateRequest request = new FamilyUpdateRequest("도토리네 가족", 10000, 5000,
			FinancialKnowledgeLevel.ADVANCED);

		assertThat(violatedFields(request)).isEmpty();
	}

	@Test
	@DisplayName("name이 공백뿐이면 위반이 발생한다")
	void validate_blankName() {
		assertThat(violatedFields(new FamilyUpdateRequest("   ", null, null, null))).contains("name");
	}

	@Test
	@DisplayName("name이 100자를 초과하면 위반이 발생한다")
	void validate_tooLongName() {
		assertThat(violatedFields(new FamilyUpdateRequest("가".repeat(101), null, null, null)))
			.contains("name");
	}

	@Test
	@DisplayName("금액은 DTO에서 검증하지 않는다 — 0원이어도 위반 없이 서비스(toPoints)로 넘어간다")
	void validate_amountsAreServiceConcern() {
		assertThat(violatedFields(new FamilyUpdateRequest(null, 0, -1000, null))).isEmpty();
	}
}
