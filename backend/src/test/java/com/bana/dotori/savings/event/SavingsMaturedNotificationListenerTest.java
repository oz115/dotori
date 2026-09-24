package com.bana.dotori.savings.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
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
class SavingsMaturedNotificationListenerTest {

	private static final Integer SUBSCRIPTION_ID = 3;
	private static final Integer CHILD_ID = 247;

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private SavingsMaturedNotificationListener listener;

	@Test
	@DisplayName("만기된 아이에게 수령액과 함께 SAVINGS 알림을 보낸다")
	void 만기_알림() {
		// 만기가 됐다고 돈이 들어오지는 않는다. 얼마가 기다리는지 보여야 받으러 온다.
		listener.onMatured(new SavingsMaturedEvent(SUBSCRIPTION_ID, CHILD_ID, "소액적금", 12000));

		then(pushNotificationService).should()
			.notify(eq(CHILD_ID), eq(NotificationType.SAVINGS), any(), contains("12000"), eq(SUBSCRIPTION_ID));
	}
}
