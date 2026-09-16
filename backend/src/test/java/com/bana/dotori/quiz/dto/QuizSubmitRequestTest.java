package com.bana.dotori.quiz.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class QuizSubmitRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(QuizSubmitRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("답안이 없으면 answer 위반이다")
	void 답안이_없으면_위반() {
		assertThat(violatedFields(new QuizSubmitRequest(null))).contains("answer");
		assertThat(violatedFields(new QuizSubmitRequest("   "))).contains("answer");
	}

	@Test
	@DisplayName("답안이 있으면 위반이 없다")
	void 정상_값() {
		assertThat(violatedFields(new QuizSubmitRequest("이자"))).isEmpty();
	}
}
