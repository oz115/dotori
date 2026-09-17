package com.bana.dotori.savings.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * DTO 계층 - 예적금 가입 요청의 필드 제약만 검증한다.
 */
@DisplayName("SavingsSubscribeRequest 검증")
class SavingsSubscribeRequestTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	@DisplayName("검증_정상_상품과_양수_금액이면_위반이_없다")
	void 검증_정상_상품과_양수_금액이면_위반이_없다() {
		Set<ConstraintViolation<SavingsSubscribeRequest>> violations =
			validator.validate(new SavingsSubscribeRequest(1, 10_000));

		assertThat(violations).isEmpty();
	}

	@ParameterizedTest(name = "금액이 {0}이면 거부한다")
	@ValueSource(ints = {0, -1, -10_000})
	@DisplayName("검증_실패_금액이_0_이하면_거부한다")
	void 검증_실패_금액이_0_이하면_거부한다(int invalidAmount) {
		Set<ConstraintViolation<SavingsSubscribeRequest>> violations =
			validator.validate(new SavingsSubscribeRequest(1, invalidAmount));

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("가입 금액은 1 이상이어야 합니다");
	}

	@Test
	@DisplayName("검증_실패_금액이_없으면_거부한다")
	void 검증_실패_금액이_없으면_거부한다() {
		Set<ConstraintViolation<SavingsSubscribeRequest>> violations =
			validator.validate(new SavingsSubscribeRequest(1, null));

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("가입 금액은 필수입니다");
	}

	@Test
	@DisplayName("검증_실패_상품을_고르지_않으면_거부한다")
	void 검증_실패_상품을_고르지_않으면_거부한다() {
		Set<ConstraintViolation<SavingsSubscribeRequest>> violations =
			validator.validate(new SavingsSubscribeRequest(null, 10_000));

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("가입할 상품을 선택해야 합니다");
	}
}
