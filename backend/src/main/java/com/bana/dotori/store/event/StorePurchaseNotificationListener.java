package com.bana.dotori.store.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bana.dotori.global.util.NotificationText;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;

import lombok.RequiredArgsConstructor;

/**
 * 구매 트랜잭션이 커밋된 뒤에만, 상품을 등록한 부모에게 알림을 보낸다 (S15P21A505-229).
 *
 * <p>AFTER_COMMIT 에서만 동작하므로, 구매 트랜잭션이 롤백되면 이 리스너 자체가 호출되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class StorePurchaseNotificationListener {

	private final PushNotificationService pushNotificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onProductPurchased(StoreProductPurchasedEvent event) {
		String title = "상점 구매 알림";
		// 상품명은 100자까지 허용돼서, 그대로 넣으면 알림함 두 줄을 이름만으로 채우고
		// "몇 개가 구매됐어요"가 화면 밖으로 밀려난다 (QA) — 이름만 줄인다.
		String body = NotificationText.shortName(event.productName())
			+ " " + event.quantity() + "개가 구매됐어요 (" + event.totalPrice() + "P)";
		pushNotificationService.notify(event.parentUserId(), NotificationType.PURCHASE, title, body,
			event.storeProductId());
	}
}
