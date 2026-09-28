package com.bana.dotori.chore.event;

/**
 * 주기가 끝나도록 부모가 처리하지 않아 완료 요청이 자동 거절됐다는 이벤트.
 *
 * <p>부모의 거절(ChoreCompletionRejectedEvent)과 나누는 이유는 아이에게 할 말이 다르기
 * 때문이다. 부모가 거절한 것은 "사진을 다시 찍어 보내라"지만, 이쪽은 아이가 잘못한 것이
 * 없고 기간이 지났을 뿐이라 "새 주기에 다시 할 수 있다"가 된다.
 */
public record ChoreCompletionExpiredEvent(
	Integer choreId,
	Integer childId,
	String choreTitle
) {
}
