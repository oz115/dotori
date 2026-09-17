package com.bana.dotori.savings.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;

import lombok.RequiredArgsConstructor;

/**
 * 예적금이 만기되면 아이에게 수령하라고 알린다.
 *
 * <p>정산 크론이 만기를 찍고 지나가는데, 수령은 아이가 직접 눌러야 한다. 이 알림이 없으면
 * 다 모은 돈이 계좌에 묶인 채로 남는다.
 *
 * <p>정산은 가입 건마다 자기 트랜잭션에서 커밋된다(REQUIRES_NEW). 그래서 한 건이 실패해
 * 롤백돼도 그 건만 알림이 나가지 않는다.
 */
@Component
@RequiredArgsConstructor
public class SavingsMaturedNotificationListener {

	private final PushNotificationService pushNotificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onMatured(SavingsMaturedEvent event) {
		pushNotificationService.notify(event.childId(), NotificationType.SAVINGS,
			"저축이 만기됐어요",
			event.productName() + " · " + event.payout() + " 도토리를 받아가세요",
			event.savingsSubscriptionId());
	}
}
