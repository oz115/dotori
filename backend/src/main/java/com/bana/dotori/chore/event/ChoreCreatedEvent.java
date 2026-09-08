package com.bana.dotori.chore.event;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;

/**
 * 할 일이 새로 등록됐다는 애플리케이션 이벤트.
 *
 * <p>서비스 트랜잭션 안에서 이 이벤트만 발행하고, 실제 RabbitMQ 발송은
 * {@link ChoreRewardEvaluationListener} 가 커밋 이후에 한다.
 * 트랜잭션이 롤백되면 애초에 리스너가 호출되지 않으므로, 저장되지 않은 할 일에 대해
 * AI 서버로 메시지가 나가는 사고를 막는다.
 *
 * <p>수정(CHORE-05)에서는 발행하지 않는다. 부모가 직접 고친 보상을 AI 가 다시
 * 덮어쓰면 수정이 되돌려진 것처럼 보인다.
 */
public record ChoreCreatedEvent(
	Integer choreId,
	Integer familyId,
	Integer childId,
	String title,
	String description,
	ChoreDifficulty difficulty,
	ChoreRepeatType repeatType
) {
}
