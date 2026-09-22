package com.bana.dotori.notification.service;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.notification.entity.DeviceToken;
import com.bana.dotori.notification.entity.Notification;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.repository.DeviceTokenRepository;
import com.bana.dotori.notification.repository.NotificationRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.repository.UserRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FCM 발송 + 알림함(notifications) 기록을 한 진입점으로 묶는다 (S15P21A505-228).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

	private final NotificationRepository notificationRepository;
	private final DeviceTokenRepository deviceTokenRepository;
	private final UserRepository userRepository;
	/**
	 * FCM 자격증명이 없는 환경에서는 이 빈이 없다 (FirebaseConfig 의 조건부 생성).
	 * 없으면 발송만 건너뛰고 알림함 기록은 그대로 남긴다.
	 */
	private final ObjectProvider<FirebaseMessaging> firebaseMessaging;

	// 호출자가 거의 항상 @TransactionalEventListener(AFTER_COMMIT) 안에서 부른다. 그 시점엔
	// 원래 트랜잭션의 리소스가 아직 스레드에 바인딩돼 있어(cleanup은 afterCommit 이후) 기본
	// REQUIRED 전파로는 이미 커밋된 트랜잭션에 조인해버려 이 메서드의 쓰기가 커밋되지 않는다.
	// REQUIRES_NEW 로 별도 트랜잭션을 강제한다.
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void notify(Integer userId, NotificationType type, String title, String body, Integer refId) {
		save(userId, type, title, body, refId);

		FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
		if (messaging == null) {
			// 로컬·테스트처럼 FCM 키가 없는 환경. 알림함에는 남았으므로 앱에서 볼 수 있다.
			return;
		}

		List<DeviceToken> deviceTokens = deviceTokenRepository.findAllByUser_Id(userId);
		if (deviceTokens.isEmpty()) {
			// 푸시 미허용이거나 등록된 기기가 없는 정상 케이스 — 알림함 기록만으로 끝낸다.
			return;
		}

		sendPush(messaging, deviceTokens, type, title, body, refId);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(Integer userId, NotificationType type, String title, String body, Integer refId) {
		save(userId, type, title, body, refId);
	}

	private void save(Integer userId, NotificationType type, String title, String body, Integer refId) {
		User user = userRepository.getReferenceById(userId);
		notificationRepository.save(Notification.builder()
			.user(user)
			.type(type)
			.title(title)
			.body(body)
			.refId(refId)
			.build());
	}

	/**
	 * data 페이로드(type/refId)는 앱이 알림을 탭했을 때 어느 화면으로 이동할지 판단하는 딥링크 정보다.
	 * FCM data 값은 전부 String 이어야 하고, refId 는 null 일 수 있어 그 경우 키 자체를 넣지 않는다.
	 */
	private void sendPush(FirebaseMessaging messaging, List<DeviceToken> deviceTokens, NotificationType type,
			String title, String body, Integer refId) {
		MulticastMessage.Builder builder = MulticastMessage.builder()
			.setNotification(com.google.firebase.messaging.Notification.builder()
				.setTitle(title)
				.setBody(body)
				.build())
			.putData("type", type.name())
			.addAllTokens(deviceTokens.stream().map(DeviceToken::getFcmToken).toList());
		if (refId != null) {
			builder.putData("refId", String.valueOf(refId));
		}
		MulticastMessage message = builder.build();

		BatchResponse response;
		try {
			response = messaging.sendEachForMulticast(message);
		} catch (FirebaseMessagingException e) {
			log.warn("FCM 발송 실패. tokenCount={}", deviceTokens.size(), e);
			return;
		}

		if (response.getFailureCount() == 0) {
			return;
		}
		cleanUpInvalidTokens(deviceTokens, response.getResponses());
	}

	/**
	 * 응답 순서는 요청에 넣은 토큰 순서와 1:1로 대응한다 (sendEachForMulticast 계약).
	 * 기기가 앱을 지웠거나 토큰이 만료된 경우(UNREGISTERED/INVALID_ARGUMENT)만 정리한다 —
	 * 일시적 오류(UNAVAILABLE 등)까지 지우면 멀쩡한 토큰을 잃는다.
	 */
	private void cleanUpInvalidTokens(List<DeviceToken> deviceTokens, List<SendResponse> responses) {
		for (int i = 0; i < responses.size(); i++) {
			SendResponse sendResponse = responses.get(i);
			if (sendResponse.isSuccessful()) {
				continue;
			}
			MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
			if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
				DeviceToken invalidToken = deviceTokens.get(i);
				log.info("무효 FCM 토큰 정리. deviceTokenId={}, errorCode={}", invalidToken.getId(), errorCode);
				deviceTokenRepository.delete(invalidToken);
			}
		}
	}
}
