package com.bana.dotori.store.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.store.config.StoreRabbitConfig;

import lombok.RequiredArgsConstructor;

/**
 * 상품 등록 트랜잭션이 커밋된 뒤에만 AI 가치 산정 큐로 메시지를 보낸다.
 *
 * <p>AFTER_COMMIT 에서만 동작하므로, 등록 트랜잭션이 롤백되면 이 리스너 자체가 호출되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class StoreProductValueEvaluationListener {

	private final RabbitTemplate rabbitTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onProductCreated(StoreProductCreatedEvent event) {
		StoreProductValueEvaluationMessage message = new StoreProductValueEvaluationMessage(
			event.storeProductId(), event.familyId(), event.name(), event.description(), event.category());
		rabbitTemplate.convertAndSend(StoreRabbitConfig.VALUE_EVALUATION_EXCHANGE,
			StoreRabbitConfig.VALUE_EVALUATION_ROUTING_KEY, message);
	}
}
