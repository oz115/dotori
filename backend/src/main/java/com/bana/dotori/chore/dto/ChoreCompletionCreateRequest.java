package com.bana.dotori.chore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 할 일 완료 요청 (CHORE-11).
 *
 * <p>{@code photoKey} 는 사진 업로드(CHORE-13)가 돌려준 저장 키다. 노션 명세는 이 필드를
 * {@code photoUrl} 이라고 적었는데, 버킷이 비공개라 URL 을 받아도 저장해 둘 수 없어 키로
 * 바꿨다 (자세한 이유는 {@code ChorePhotoKey} 주석 참고).
 *
 * <p>사진은 <b>필수</b>다. 업로드 API 가 없던 동안만 선택으로 뒀던 것이라 되돌렸다.
 * 아이는 사진과 내용을 한 번에 올려 요청을 완성하고, 올린 뒤에는 고칠 수 없다.
 * 부모가 거절하면 같은 요청을 손보는 게 아니라 다시 올려 새 요청을 만든다.
 *
 * <p>형식이 맞는지는 여기서 보지 않는다. 그 키가 <b>이 아이의 것</b>인지까지 확인해야 하고
 * 그러려면 경로의 familyId·childId 를 알아야 하는데, 본문 검증은 그 값을 모른다.
 * 서비스가 {@code ChorePhotoKey#belongsTo} 로 확인한다.
 *
 * <p>길이 제한은 DDL 을 따른다 — photo_key VARCHAR(100), description VARCHAR(255).
 * 여기서 걸러 내지 않으면 DB 가 22001 로 던지고 500 이 나간다.
 */
public record ChoreCompletionCreateRequest(
	@NotBlank(message = "인증 사진이 필요합니다")
	@Size(max = 100, message = "사진 키는 100자 이하여야 합니다")
	String photoKey,

	@Size(max = 255, message = "요청 메시지는 255자 이하여야 합니다")
	String description
) {
}
