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
			.notify(eq(1), eq(NotificationType.PURCHASE), any(), any(), eq(5));
	}
}
