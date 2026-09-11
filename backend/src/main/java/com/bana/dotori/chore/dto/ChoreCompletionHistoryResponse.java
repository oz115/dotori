package com.bana.dotori.chore.dto;

import java.util.List;

/**
 * 완료 내역 목록 (CHORE-03).
 *
 * <p>합계는 담지 않는다. 이 응답은 기간 전체를 한 번에 주므로 클라이언트가 배열을 더하면
 * 되고, 원장을 집계해 두면 나중에 원장과 합계가 어긋날 자리가 생긴다.
 * 필요해지면 그때 붙인다.
 */
public record ChoreCompletionHistoryResponse(
	List<ChoreCompletionHistoryItem> completions
) {
}
