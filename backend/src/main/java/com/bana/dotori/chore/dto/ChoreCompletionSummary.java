package com.bana.dotori.chore.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreCompletionStatus;

/**
 * 부모 대기함의 한 줄 (CHORE-08).
 *
 * <p>choreTitle 과 childName 은 완료 요청 행에 없다. 부모가 목록만 보고 "누가 무엇을
 * 했다는 것인지" 알 수 있어야 해서 chores·users 에서 끌어온다.
 *
 * <p>{@code photoUrl} 은 <b>presigned URL</b> 이다. DB 에 저장된 것은 키이고
 * ({@link ChoreCompletionRow}), 서비스가 응답을 만들 때 그 키로 링크를 발급해 여기 담는다.
 * 버킷이 비공개라 이 링크 없이는 사진을 볼 수 없다.
 *
 * <p><b>몇 분 뒤 만료된다.</b> 목록을 오래 캐시해 두면 이미지가 403 으로 깨지므로,
 * 클라이언트는 화면에 들어올 때마다 목록을 다시 불러오거나 로드 실패 시 재조회해야 한다.
 */
public record ChoreCompletionSummary(
	Integer choreCompletionRequestId,
	Integer choreId,
	String choreTitle,
	String childName,
	String photoUrl,
	String description,
	ChoreCompletionStatus status,
	OffsetDateTime requestedAt
) {
}
