package com.bana.dotori.chore.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ChoreCompletionCreateRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(ChoreCompletionCreateRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("사진과 메시지가 모두 없어도 위반이 아니다")
	void 둘_다_비어도_통과() {
		// 사진 업로드 API(CHORE-13)가 아직 없어 photoUrl 을 필수로 걸 수 없다.
		assertThat(violatedFields(new ChoreCompletionCreateRequest(null, null))).isEmpty();
	}

	@Test
	@DisplayName("두 값이 길이 안에 들면 위반이 없다")
	void 정상_값() {
		ChoreCompletionCreateRequest request =
			new ChoreCompletionCreateRequest("https://cdn.example.com/chores/1.jpg", "다 했어요");

		assertThat(violatedFields(request)).isEmpty();
	}

	@Test
	@DisplayName("사진 URL이 100자를 넘으면 photoUrl 위반이다")
	void 사진_URL이_너무_길다() {
		// DDL 이 VARCHAR(100) 이라 여기서 안 막으면 DB 가 22001 로 던지고 500 이 나간다.
		String tooLong = "h".repeat(101);

		assertThat(violatedFields(new ChoreCompletionCreateRequest(tooLong, null))).containsExactly("photoUrl");
	}

	@Test
	@DisplayName("메시지가 255자를 넘으면 description 위반이다")
	void 메시지가_너무_길다() {
		String tooLong = "가".repeat(256);

		assertThat(violatedFields(new ChoreCompletionCreateRequest(null, tooLong))).containsExactly("description");
	}

	@Test
	@DisplayName("경계값은 통과한다")
	void 경계값은_통과한다() {
		ChoreCompletionCreateRequest request =
			new ChoreCompletionCreateRequest("h".repeat(100), "가".repeat(255));

		assertThat(violatedFields(request)).isEmpty();
	}
}
