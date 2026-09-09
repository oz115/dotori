package com.bana.dotori.chore.service;

import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.dto.ChoreCompletionListResponse;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;

/**
 * 집안일 완료 인증 (CHORE-08·11·12).
 *
 * <p>아이가 올리고 물리는 것(11·12)과 부모가 들여다보는 대기함(08)을 한 서비스에 둔다.
 * 같은 테이블을 같은 규칙으로 다루기 때문이다. 승인·거절(CHORE-09·10)은 보상 지급이
 * 얽혀 트랜잭션 범위가 달라지므로 그때 나눈다.
 */
public interface ChoreCompletionService {

	ChoreCompletionResponse requestCompletion(Integer userId, Integer familyId, Integer childId, Integer choreId,
			ChoreCompletionCreateRequest request);

	void cancelCompletion(Integer userId, Integer familyId, Integer childId, Integer requestId);

	ChoreCompletionListResponse getCompletionRequests(Integer userId, Integer familyId, Integer childId,
			ChoreCompletionStatus status);
}
