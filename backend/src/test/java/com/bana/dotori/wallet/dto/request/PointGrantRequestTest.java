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
 * DTO 계층 - 지급 요청의 필드 제약과 childId 일치 판단을 검증한다.
 *
 * <p>childId 는 경로와 본문 양쪽에 올 수 있어, 두 값이 어긋났는지 판단하는 규칙이
 * DTO 안에 들어 있다. 잘못 판단하면 엉뚱한 아이에게 포인트가 발행되므로 따로 검증한다.
 */
@DisplayName("PointGrantRequest 검증")
class PointGrantRequestTest {

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
	@DisplayName("검증_정상_필수값이_채워지면_위반이_없다")
	void 검증_정상_필수값이_채워지면_위반이_없다() {
		Set<ConstraintViolation<PointGrantRequest>> violations =
				validator.validate(new PointGrantRequest(2, 5_000, "용돈"));

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("검증_정상_사유는_없어도_된다")
	void 검증_정상_사유는_없어도_된다() {
		assertThat(validator.validate(new PointGrantRequest(2, 5_000, null))).isEmpty();
	}

	@ParameterizedTest(name = "금액이 {0}이면 거부한다")
	@ValueSource(ints = {0, -1})
	@DisplayName("검증_실패_금액이_0_이하면_거부한다")
	void 검증_실패_금액이_0_이하면_거부한다(int invalidAmount) {
		Set<ConstraintViolation<PointGrantRequest>> violations =
				validator.validate(new PointGrantRequest(2, invalidAmount, "용돈"));

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("지급 포인트는 1 이상이어야 합니다");
	}

	@Test
	@DisplayName("검증_실패_금액이_없으면_거부한다")
	void 검증_실패_금액이_없으면_거부한다() {
		Set<ConstraintViolation<PointGrantRequest>> violations =
				validator.validate(new PointGrantRequest(2, null, "용돈"));

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("지급 포인트는 필수입니다");
	}

	@Test
	@DisplayName("검증_실패_사유가_255자를_넘으면_거부한다")
	void 검증_실패_사유가_255자를_넘으면_거부한다() {
		String tooLong = "가".repeat(256);

		Set<ConstraintViolation<PointGrantRequest>> violations =
				validator.validate(new PointGrantRequest(2, 5_000, tooLong));

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("지급 사유는 255자를 넘을 수 없습니다");
	}

	@Test
	@DisplayName("childId일치_본문값이_경로와_다르면_어긋난_것으로_본다")
	void childId일치_본문값이_경로와_다르면_어긋난_것으로_본다() {
		assertThat(new PointGrantRequest(99, 5_000, "용돈").conflictsWith(2))
				.as("이걸 놓치면 엉뚱한 아이에게 포인트가 발행된다")
				.isTrue();
	}

	@Test
	@DisplayName("childId일치_본문값이_경로와_같으면_통과한다")
	void childId일치_본문값이_경로와_같으면_통과한다() {
		assertThat(new PointGrantRequest(2, 5_000, "용돈").conflictsWith(2)).isFalse();
	}

	@Test
	@DisplayName("childId일치_본문에_값이_없으면_경로값을_그대로_쓴다")
	void childId일치_본문에_값이_없으면_경로값을_그대로_쓴다() {
		assertThat(new PointGrantRequest(null, 5_000, "용돈").conflictsWith(2)).isFalse();
	}
}
