package com.bana.dotori.chore.event;

/**
 * 부모가 완료를 승인하고 보상까지 지급됐다는 애플리케이션 이벤트 (CHORE-14).
 *
 * <p>지급이 롤백되면 리스너가 호출되지 않는다. "도토리를 받았어요" 가 나갔는데 실제로는
 * 안 들어와 있는 상황을 막으려면 반드시 커밋 이후여야 한다.
 *
 * @param rewardPoints 이번에 실제로 지급된 금액. 할 일의 현재 보상값과 다를 수 있어 따로 싣는다
 */
public record ChoreCompletionApprovedEvent(
	Integer choreId,
	Integer childId,
	String choreTitle,
	Integer rewardPoints
) {
}
