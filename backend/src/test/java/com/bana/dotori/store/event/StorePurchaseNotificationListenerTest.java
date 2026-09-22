package com.bana.dotori.store.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;

@ExtendWith(MockitoExtension.class)
class StorePurchaseNotificationListenerTest {

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private StorePurchaseNotificationListener listener;

	@Test
	@DisplayName("구매 이벤트를 받으면 상품을 등록한 부모에게 PURCHASE 알림을 보낸다")
	void onProductPurchased_notifiesRegisteringParent() {
		StoreProductPurchasedEvent event = new StoreProductPurchasedEvent(77, 5, 1, "자전거", 1, 900000);

		listener.onProductPurchased(event);

		then(pushNotificationService).should()
			.notify(eq(1), eq(NotificationType.PURCHASE), any(), eq("자전거 1개가 구매됐어요 (900000P)"), eq(5));
	}

	@Test
	@DisplayName("긴 상품명은 줄여서 담는다 — 몇 개가 구매됐는지가 밀려나지 않게")
	void onProductPurchased_shortensLongName() {
		// 상품명은 100자까지 허용된다. 그대로 넣으면 알림함 두 줄을 이름만으로 채워서
		// "1개가 구매됐어요" 가 안 보인다 (QA).
		StoreProductPurchasedEvent event = new StoreProductPurchasedEvent(77, 5, 1, "가".repeat(40), 1, 900000);

		listener.onProductPurchased(event);

		then(pushNotificationService).should()
			.notify(eq(1), eq(NotificationType.PURCHASE), any(),
				eq("가".repeat(15) + "... 1개가 구매됐어요 (900000P)"), eq(5));
	}
}
