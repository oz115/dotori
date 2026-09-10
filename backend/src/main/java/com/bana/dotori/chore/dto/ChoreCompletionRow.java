package com.bana.dotori.chore.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreCompletionStatus;

/**
 * 부모 대기함 조회의 리포지토리 결과 한 줄 (CHORE-08 내부용).
 *
 * <p>응답으로 나가는 {@link ChoreCompletionSummary} 와 딱 하나가 다르다 — 여기에는 저장된
 * <b>키</b>가 들어 있고, 응답에는 그 키로 발급한 presigned <b>URL</b>이 들어간다.
 *
 * <p>타입을 나눈 이유는 키가 응답에 새어 나가지 않게 하려는 것이다. 한 record 에 두 필드를
 * 다 두면 매핑을 깜빡한 경로에서 키가 그대로 클라이언트로 나간다. 키는 서버만 알아야 하는
 * 값은 아니지만, 클라이언트가 그걸로 할 수 있는 일이 없어 내보낼 이유도 없다.
 */
public record ChoreCompletionRow(
	Integer choreCompletionRequestId,
	Integer choreId,
	String choreTitle,
	String childName,
	String photoKey,
	String description,
	ChoreCompletionStatus status,
	OffsetDateTime requestedAt
) {
}
