package com.bana.dotori.chore.dto;

import java.time.OffsetDateTime;

/**
 * 완료 내역 한 줄 (CHORE-03).
 *
 * <p>출처가 {@code chore_completion_requests} 가 아니라 {@code point_transactions} 다.
 * 요청 행에는 지급 금액이 없고, 승인 시점의 {@code chores.reward_points} 를 읽으면
 * 부모가 그 뒤 보상을 고쳤을 때 과거 이력이 통째로 바뀐다. 지갑 원장은 모든 필드가
 * {@code updatable = false} 라 한 번 쓰이면 못 바뀌므로, 그쪽 금액이 실제로 받은 값이다.
 *
 * <p>그래서 완료 요청 ID 는 담지 않는다. 지급 거래는 (집안일, 주기)를 가리키고
 * 요청 행을 가리키지 않아 안전하게 이어붙일 방법이 없다.
 */
public record ChoreCompletionHistoryItem(
	Integer choreId,
	String choreTitle,
	String childName,
	Integer rewardPoints,
	OffsetDateTime processedAt
) {
}
