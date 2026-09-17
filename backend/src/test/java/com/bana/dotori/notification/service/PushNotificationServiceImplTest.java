package com.bana.dotori.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.notification.entity.DeviceToken;
import com.bana.dotori.notification.entity.DeviceTokenPlatform;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.repository.DeviceTokenRepository;
import com.bana.dotori.notification.repository.NotificationRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.repository.UserRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceImplTest {

	private static final Integer USER_ID = 1;

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private DeviceTokenRepository deviceTokenRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FirebaseMessaging firebaseMessaging;

	/**
	 * FCM 자격증명이 없는 환경에서는 빈이 없을 수 있어 서비스가 ObjectProvider 로 받는다.
	 * 여기서는 항상 있는 환경을 흉내 낸다 - 빈이 없는 경우는 따로 검증한다.
	 */
	@Mock
	private ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

	@InjectMocks
	private PushNotificationServiceImpl pushNotificationService;

	@BeforeEach
	void provideFirebaseMessaging() {
		given(firebaseMessagingProvider.getIfAvailable()).willReturn(firebaseMessaging);
	}

	private User userReference() {
		User user = User.builder().loginId("login").password("pw").name("이름").email("a@a.com").build();
		ReflectionTestUtils.setField(user, "id", USER_ID);
		return user;
	}

	private DeviceToken deviceToken(Integer id, String token) {
		User user = userReference();
		DeviceToken deviceToken = DeviceToken.builder().user(user).fcmToken(token).platform(DeviceTokenPlatform.ANDROID)
			.build();
		ReflectionTestUtils.setField(deviceToken, "id", id);
		return deviceToken;
	}

	@Test
	@DisplayName("등록된 기기가 없으면 알림함에만 기록하고 FCM은 보내지 않는다")
	void notify_noDeviceTokens_onlyStoresNotification() throws Exception {
		given(userRepository.getReferenceById(USER_ID)).willReturn(userReference());
		given(deviceTokenRepository.findAllByUser_Id(USER_ID)).willReturn(List.of());

		pushNotificationService.notify(USER_ID, NotificationType.PURCHASE, "제목", "내용", 5);

		then(notificationRepository).should().save(any());
		then(firebaseMessaging).should(never()).sendEachForMulticast(any());
	}

	@Test
	@DisplayName("등록된 기기가 있으면 알림함 기록과 함께 FCM 멀티캐스트를 보낸다")
	void notify_withDeviceTokens_sendsMulticast() throws Exception {
		given(userRepository.getReferenceById(USER_ID)).willReturn(userReference());
		given(deviceTokenRepository.findAllByUser_Id(USER_ID)).willReturn(List.of(deviceToken(1, "token-a")));
		BatchResponse response = mock(BatchResponse.class);
		given(response.getFailureCount()).willReturn(0);
		given(firebaseMessaging.sendEachForMulticast(any())).willReturn(response);

		pushNotificationService.notify(USER_ID, NotificationType.PURCHASE, "제목", "내용", 5);

		then(notificationRepository).should().save(any());
		then(firebaseMessaging).should().sendEachForMulticast(any());
		then(deviceTokenRepository).should(never()).delete(any());
	}

	@Test
	@DisplayName("UNREGISTERED 에러가 난 토큰은 정리하고, 성공한 토큰은 그대로 둔다")
	void notify_cleansUpUnregisteredTokenOnly() throws Exception {
		DeviceToken validToken = deviceToken(1, "token-valid");
		DeviceToken deadToken = deviceToken(2, "token-dead");
		given(userRepository.getReferenceById(USER_ID)).willReturn(userReference());
		given(deviceTokenRepository.findAllByUser_Id(USER_ID)).willReturn(List.of(validToken, deadToken));

		SendResponse success = mock(SendResponse.class);
		given(success.isSuccessful()).willReturn(true);

		FirebaseMessagingException unregisteredError = mock(FirebaseMessagingException.class);
		given(unregisteredError.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
		SendResponse failure = mock(SendResponse.class);
		given(failure.isSuccessful()).willReturn(false);
		given(failure.getException()).willReturn(unregisteredError);

		BatchResponse response = mock(BatchResponse.class);
		given(response.getFailureCount()).willReturn(1);
		given(response.getResponses()).willReturn(List.of(success, failure));
		given(firebaseMessaging.sendEachForMulticast(any())).willReturn(response);

		pushNotificationService.notify(USER_ID, NotificationType.PURCHASE, "제목", "내용", 5);

		then(deviceTokenRepository).should().delete(deadToken);
		then(deviceTokenRepository).should(never()).delete(validToken);
	}

	@Test
	@DisplayName("일시적 오류(UNAVAILABLE)로 실패한 토큰은 지우지 않는다")
	void notify_doesNotCleanUpTransientFailure() throws Exception {
		DeviceToken token = deviceToken(1, "token-a");
		given(userRepository.getReferenceById(USER_ID)).willReturn(userReference());
		given(deviceTokenRepository.findAllByUser_Id(USER_ID)).willReturn(List.of(token));

		FirebaseMessagingException transientError = mock(FirebaseMessagingException.class);
		given(transientError.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNAVAILABLE);
		SendResponse failure = mock(SendResponse.class);
		given(failure.isSuccessful()).willReturn(false);
		given(failure.getException()).willReturn(transientError);

		BatchResponse response = mock(BatchResponse.class);
		given(response.getFailureCount()).willReturn(1);
		given(response.getResponses()).willReturn(List.of(failure));
		given(firebaseMessaging.sendEachForMulticast(any())).willReturn(response);

		pushNotificationService.notify(USER_ID, NotificationType.PURCHASE, "제목", "내용", 5);

		then(deviceTokenRepository).should(never()).delete(any());
	}

	@Test
	@DisplayName("FCM 발송 자체가 예외를 던지면 삼키고 알림함 기록은 유지된다")
	void notify_fcmThrows_doesNotPropagate() throws Exception {
		given(userRepository.getReferenceById(USER_ID)).willReturn(userReference());
		given(deviceTokenRepository.findAllByUser_Id(USER_ID)).willReturn(List.of(deviceToken(1, "token-a")));
		given(firebaseMessaging.sendEachForMulticast(any()))
			.willThrow(mock(FirebaseMessagingException.class));

		pushNotificationService.notify(USER_ID, NotificationType.PURCHASE, "제목", "내용", 5);

		then(notificationRepository).should().save(any());
	}
}
