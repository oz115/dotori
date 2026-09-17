package com.bana.dotori.chore.event;

/**
 * 부모가 완료를 거절했다는 애플리케이션 이벤트 (CHORE-15).
 *
 * <p>거절 사유는 싣지 않는다 — 서버가 저장하지 않아 담을 값이 없다. 아이에게는 같은
 * 주기에 다시 요청할 수 있다는 것만 알리면 된다.
 */
public record ChoreCompletionRejectedEvent(
	Integer choreId,
	Integer childId,
	String choreTitle
) {
}
