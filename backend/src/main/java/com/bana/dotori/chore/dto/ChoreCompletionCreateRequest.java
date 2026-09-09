package com.bana.dotori.chore.dto;

import jakarta.validation.constraints.Size;

/**
 * 할 일 완료 요청 (CHORE-11).
 *
 * <p>photoUrl 은 노션 명세상 필수지만 여기서는 선택으로 둔다. 사진 업로드 API(CHORE-13)가
 * 아직 없어 클라이언트가 URL 을 만들 방법이 없고, 필수로 걸면 완료 요청 자체를 못 보낸다.
 * CHORE-13 이 생기면 {@code @NotBlank} 로 바꾼다.
 *
 * <p>길이 제한은 DDL 을 따른다 — photo_url VARCHAR(100), description VARCHAR(255).
 * 여기서 걸러 내지 않으면 DB 가 22001 로 던지고 500 이 나간다.
 */
public record ChoreCompletionCreateRequest(
	@Size(max = 100, message = "사진 URL은 100자 이하여야 합니다")
	String photoUrl,

	@Size(max = 255, message = "요청 메시지는 255자 이하여야 합니다")
	String description
) {
}
