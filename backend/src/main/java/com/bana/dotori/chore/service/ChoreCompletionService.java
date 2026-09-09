package com.bana.dotori.chore.service;

import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;

/**
 * 집안일 완료 인증 (CHORE-11·12).
 *
 * <p>승인·거절(CHORE-09·10)과 목록 조회(CHORE-08)는 부모 쪽 동작이라 별도 서비스로 나눈다.
 */
public interface ChoreCompletionService {

	ChoreCompletionResponse requestCompletion(Integer userId, Integer familyId, Integer childId, Integer choreId,
			ChoreCompletionCreateRequest request);

	void cancelCompletion(Integer userId, Integer familyId, Integer childId, Integer requestId);
}
