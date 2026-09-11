package com.bana.dotori.chore.dto;

import java.util.List;

/**
 * 완료 요청 목록 (CHORE-08).
 *
 * <p>배열을 그대로 내보내지 않고 객체로 한 겹 감싼다 — 나중에 페이징 정보를 붙일 때
 * 응답 모양을 바꾸지 않아도 된다. 집안일 목록(ChoreListResponse)과 같은 방식이다.
 */
public record ChoreCompletionListResponse(
	List<ChoreCompletionSummary> requests
) {
}
