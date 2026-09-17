package com.bana.dotori.chore.event;

/**
 * 아이가 완료를 인증했다는 애플리케이션 이벤트 (CHORE-11).
 *
 * <p>서비스 트랜잭션 안에서 발행만 하고, 알림 발송은 리스너가 커밋 이후에 한다.
 * 롤백되면 리스너가 아예 호출되지 않으므로, 저장되지 않은 요청에 대해 부모에게
 * 알림이 나가는 사고를 막는다.
 *
 * @param parentId 알림을 받을 부모. 완료 요청에는 부모가 없어서 할 일에서 가져온다
 */
public record ChoreCompletionRequestedEvent(
	Integer choreId,
	Integer parentId,
	String choreTitle,
	String childName
) {
}
