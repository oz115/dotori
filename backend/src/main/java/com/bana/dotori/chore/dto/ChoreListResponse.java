package com.bana.dotori.chore.dto;

import java.util.List;

/**
 * 집안일 목록 (CHORE-01).
 *
 * <p>활성 집안일만 담는다. 가족당 수십 건 수준이라 페이징을 두지 않았다.
 */
public record ChoreListResponse(
	List<ChoreSummary> chores
) {
}
