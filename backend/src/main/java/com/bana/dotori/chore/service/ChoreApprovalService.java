package com.bana.dotori.chore.service;

import com.bana.dotori.chore.dto.ChoreApprovalResponse;

/**
 * 완료 승인과 보상 지급 (CHORE-09 + CHORE-07).
 *
 * <p>요청·취소·조회({@link ChoreCompletionService})와 나눠 둔 이유는 지갑이 얽히기
 * 때문이다. 이쪽은 포인트 지급까지 한 트랜잭션으로 묶어야 하므로 의존성과 실패 양상이
 * 다르고, 동시성도 여기만 문제가 된다.
 */
public interface ChoreApprovalService {

	ChoreApprovalResponse approve(Integer userId, Integer familyId, Integer childId, Integer requestId);
}
