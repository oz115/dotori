package com.bana.dotori.chore.dto;

/**
 * 인증 사진 업로드 결과 (CHORE-13).
 *
 * <p>키만 돌려준다. 노션 명세는 {@code photoUrl} 인데, 비공개 버킷이라 URL 을 줘도 그 URL 로는
 * 아무도 볼 수 없고 볼 수 있는 presigned URL 은 몇 분 뒤 만료돼 완료 요청에 실어 보낼 값이
 * 못 된다. 그리고 아이는 방금 자기 카메라로 찍었으므로 미리보기는 기기에 있는 파일로 하면
 * 된다 — 서버가 방금 받은 사진을 URL 로 되돌려줄 이유가 없다.
 *
 * <p>이 키를 완료 요청(CHORE-11)의 {@code photoKey} 에 그대로 실어 보낸다.
 */
public record ChorePhotoUploadResponse(
	String photoKey
) {
}
