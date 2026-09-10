package com.bana.dotori.chore.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.chore.entity.ChoreCompletionStatus;

/**
 * 완료 거절 결과 (CHORE-10).
 *
 * <p>거절 사유는 받지도 남기지도 않는다. 노션 명세에는 {@code reason} 본문이 있지만
 * 그것을 담을 컬럼이 없고(아이가 쓴 description 을 덮어쓸 수는 없다), 사유가 향할 곳인
 * 알림 도메인도 아직 없다. 팀 결정에 따라 이번 범위에서 뺐다 — 명세도 같이 고쳐야 한다.
 *
 * <p>거절당한 아이는 같은 주기에 다시 요청할 수 있다. 막는 것은 지급이지 요청이 아니다.
 */
public record ChoreRejectionResponse(
	Integer choreCompletionRequestId,
	ChoreCompletionStatus status,
	OffsetDateTime processedAt
) {
}
