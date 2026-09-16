package com.bana.dotori.investment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class InvestmentOrderRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(InvestmentOrderRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("종목 ID가 있으면 위반이 없다")
	void validate_success() {
		InvestmentOrderRequest request = new InvestmentOrderRequest(1);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("종목 ID가 없으면 위반이 발생한다")
	void validate_nullProductId() {
		InvestmentOrderRequest request = new InvestmentOrderRequest(null);

		assertThat(violatedFields(request)).contains("investmentProductId");
	}
}
