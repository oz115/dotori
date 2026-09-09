package com.bana.dotori.chore.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreCompletionStatus;

/**
 * 부모 대기함의 한 줄 (CHORE-08).
 *
 * <p>choreTitle 과 childName 은 완료 요청 행에 없다. 부모가 목록만 보고 "누가 무엇을
 * 했다는 것인지" 알 수 있어야 해서 chores·users 에서 끌어온다.
 *
 * <p>이 record 는 리포지토리의 JPQL 생성자 표현식이 직접 채운다. 요청을 먼저 읽고
 * 제목·이름을 건당 다시 조회하면 대기함 한 화면에 쿼리가 수십 번 나간다.
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
