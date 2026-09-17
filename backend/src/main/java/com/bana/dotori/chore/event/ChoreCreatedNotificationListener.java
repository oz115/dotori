package com.bana.dotori.chore.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;

import lombok.RequiredArgsConstructor;

/**
 * 할 일이 등록되면 담당 아이에게 알린다 (S15P21A505-243).
 *
 * <p>{@link ChoreCreatedEvent} 는 AI 보상 산정({@link ChoreRewardEvaluationListener})이 이미
 * 쓰고 있다. 같은 이벤트를 구독만 하나 더 붙였다 — 등록이라는 사실은 하나인데 이벤트를
 * 둘로 나누면 발행 지점이 늘어나고 한쪽만 빠뜨리기 쉽다.
 *
 * <p>등록 시점에 알리므로 아이가 들어왔을 때 보상이 아직 "산정 중" 일 수 있다. 그때는
 * 완료 요청을 보낼 수 없지만(REWARD_NOT_EVALUATED), 할 일이 생겼다는 사실 자체는 바로
 * 아는 편이 낫다고 보고 이렇게 뒀다.
 */
@Component
@RequiredArgsConstructor
public class ChoreCreatedNotificationListener {

	private final PushNotificationService pushNotificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onChoreCreated(ChoreCreatedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.CHORE,
			"새 할 일이 생겼어요", event.title(), event.choreId());
	}
}
