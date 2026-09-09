package com.bana.dotori.chore.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.chore.config.ChoreRabbitConfig;

import lombok.RequiredArgsConstructor;

/**
 * 할 일 등록 트랜잭션이 커밋된 뒤에만 AI 보상 산정 큐로 메시지를 보낸다.
 *
 * <p>AFTER_COMMIT 에서만 동작하므로, 등록 트랜잭션이 롤백되면 이 리스너 자체가 호출되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ChoreRewardEvaluationListener {

	private final RabbitTemplate rabbitTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onChoreCreated(ChoreCreatedEvent event) {
		ChoreRewardEvaluationMessage message = new ChoreRewardEvaluationMessage(
			event.choreId(), event.familyId(), event.childId(), event.title(), event.description(),
			event.difficulty(), event.repeatType());
		rabbitTemplate.convertAndSend(ChoreRabbitConfig.REWARD_EVALUATION_EXCHANGE,
			ChoreRabbitConfig.REWARD_EVALUATION_ROUTING_KEY, message);
	}
}
