package com.bana.dotori.chore.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreCompletionStatus;

/**
 * 완료 승인 결과 (CHORE-09).
 *
 * <p>rewardPoints 는 이번에 실제로 지급된 금액이다. 승인 시점의
 * {@code chores.reward_points} 를 그대로 준다 — 요청 시점 값을 스냅샷하지 않기로 했으므로
 * 대기 중에 부모가 금액을 고쳤다면 고친 값이 나간다.
 *
 * <p>status 는 SUCCEED 다. 노션 명세에는 APPROVED 로 적혀 있지만 DB ENUM 이 원본이라는
 * 팀 결정에 따라 SUCCEED 를 쓴다 (다른 요청 상태 ENUM 도 모두 SUCCEED 다).
 */
public record ChoreApprovalResponse(
	Integer choreCompletionRequestId,
	ChoreCompletionStatus status,
	Integer rewardPoints,
	OffsetDateTime processedAt
) {
}
