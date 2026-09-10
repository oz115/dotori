package com.bana.dotori.chore.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreCompletionStatus;

/**
 * 완료 요청 생성 결과 (CHORE-11).
 *
 * <p>보상 금액은 담지 않는다. 지급액은 승인 시점의 {@code chores.reward_points} 라서
 * 요청 시점에 알려 주면 그때까지 부모가 금액을 고쳤을 경우 거짓말이 된다.
 */
public record ChoreCompletionResponse(
	Integer choreCompletionRequestId,
	Integer choreId,
	ChoreCompletionStatus status,
	OffsetDateTime requestedAt
) {
}
