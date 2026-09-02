package com.bana.dotori.wallet.dto.request;

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
 * DTO 계층 - 충전 요청의 필드 제약만 검증한다.
 *
 * <p>스프링을 띄우지 않고 Validator 로 직접 돌린다. 여기가 실패하면 원인은
 * 비즈니스 로직이 아니라 애노테이션 선언이다.
 */
@DisplayName("PointChargeRequest 검증")
class PointChargeRequestTest {

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
	@DisplayName("검증_정상_금액이_양수면_위반이_없다")
	void 검증_정상_금액이_양수면_위반이_없다() {
		Set<ConstraintViolation<PointChargeRequest>> violations =
				validator.validate(new PointChargeRequest(10_000));

		assertThat(violations).isEmpty();
	}

	@ParameterizedTest(name = "금액이 {0}이면 거부한다")
	@ValueSource(ints = {0, -1, -10_000})
	@DisplayName("검증_실패_금액이_0_이하면_거부한다")
	void 검증_실패_금액이_0_이하면_거부한다(int invalidAmount) {
		Set<ConstraintViolation<PointChargeRequest>> violations =
				validator.validate(new PointChargeRequest(invalidAmount));

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("충전 금액은 1 이상이어야 합니다");
	}

	@Test
	@DisplayName("검증_실패_금액이_없으면_거부한다")
	void 검증_실패_금액이_없으면_거부한다() {
		Set<ConstraintViolation<PointChargeRequest>> violations =
				validator.validate(new PointChargeRequest(null));

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("충전 금액은 필수입니다");
	}
}
