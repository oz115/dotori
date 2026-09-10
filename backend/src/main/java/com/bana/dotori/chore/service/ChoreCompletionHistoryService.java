package com.bana.dotori.chore.service;

import java.time.LocalDate;

import com.bana.dotori.chore.dto.ChoreCompletionHistoryResponse;

/**
 * 승인 완료된 집안일 이력 (CHORE-03).
 *
 * <p>아이에게는 "내가 이만큼 했다", 부모에게는 "이만큼 줬다" 를 보여 주는 화면이다.
 *
 * <p>다른 완료 API 와 달리 {@code chore_completion_requests} 가 아니라
 * {@code point_transactions} 를 읽는다. 실제로 지급된 금액이 거기에만 있기 때문이다.
 */
public interface ChoreCompletionHistoryService {

	ChoreCompletionHistoryResponse getCompletions(Integer userId, Integer familyId, Integer childId,
			LocalDate startDate, LocalDate endDate);
}
