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
	@DisplayName("사진 키가 없으면 photoKey 위반이다")
	void 사진이_없으면_위반() {
		// 아이는 사진과 내용을 한 번에 올려 요청을 완성한다. 사진 없이 올릴 수는 없다.
		assertThat(violatedFields(new ChoreCompletionCreateRequest(null, null))).contains("photoKey");
		assertThat(violatedFields(new ChoreCompletionCreateRequest("   ", null))).contains("photoKey");
	}

	@Test
	@DisplayName("두 값이 길이 안에 들면 위반이 없다")
	void 정상_값() {
		ChoreCompletionCreateRequest request =
			new ChoreCompletionCreateRequest("chores/10/2/550e8400-e29b-41d4-a716-446655440000.jpg", "다 했어요");

		assertThat(violatedFields(request)).isEmpty();
	}

	@Test
	@DisplayName("사진 키가 100자를 넘으면 photoKey 위반이다")
	void 사진_키가_너무_길다() {
		// DDL 의 photo_key 가 VARCHAR(100) 이라 여기서 안 막으면 DB 가 22001 로 던지고 500 이 나간다.
		String tooLong = "h".repeat(101);

		assertThat(violatedFields(new ChoreCompletionCreateRequest(tooLong, null))).containsExactly("photoKey");
	}

	@Test
	@DisplayName("메시지가 255자를 넘으면 description 위반이다")
	void 메시지가_너무_길다() {
		String tooLong = "가".repeat(256);
		String validKey = "chores/10/2/550e8400-e29b-41d4-a716-446655440000.jpg";

		assertThat(violatedFields(new ChoreCompletionCreateRequest(validKey, tooLong)))
			.containsExactly("description");
	}

	@Test
	@DisplayName("경계값은 통과한다")
	void 경계값은_통과한다() {
		ChoreCompletionCreateRequest request =
			new ChoreCompletionCreateRequest("h".repeat(100), "가".repeat(255));

		assertThat(violatedFields(request)).isEmpty();
	}
}
