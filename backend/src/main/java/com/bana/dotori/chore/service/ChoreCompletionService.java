package com.bana.dotori.chore.service;

import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.dto.ChoreCompletionListResponse;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.dto.ChoreRejectionResponse;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;

/**
 * 집안일 완료 인증 (CHORE-08·10·11·12).
 *
 * <p>아이가 올리고 물리는 것(11·12), 부모가 들여다보는 대기함(08), 부모의 거절(10)을
 * 한 서비스에 둔다. 같은 테이블을 같은 규칙으로 다루고 지갑이 얽히지 않기 때문이다.
 *
 * <p>승인(CHORE-09)만 {@link ChoreApprovalService} 로 뺐다. 그쪽은 포인트 지급까지
 * 한 트랜잭션으로 묶어야 해서 의존성과 실패 양상이 다르고, 동시성도 거기만 문제가 된다.
 */
public interface ChoreCompletionService {

	ChoreCompletionResponse requestCompletion(Integer userId, Integer familyId, Integer childId, Integer choreId,
			ChoreCompletionCreateRequest request);

	void cancelCompletion(Integer userId, Integer familyId, Integer childId, Integer requestId);

	ChoreCompletionListResponse getCompletionRequests(Integer userId, Integer familyId, Integer childId,
			ChoreCompletionStatus status);

	ChoreRejectionResponse rejectCompletion(Integer userId, Integer familyId, Integer childId, Integer requestId);
}
