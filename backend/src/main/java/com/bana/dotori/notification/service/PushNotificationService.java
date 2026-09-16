package com.bana.dotori.notification.service;

import com.bana.dotori.notification.entity.NotificationType;

public interface PushNotificationService {

	/**
	 * 알림함(notifications 테이블)에 기록하고, 그 사용자의 등록된 기기로 FCM 푸시를 보낸다.
	 *
	 * <p>등록된 기기가 없으면(푸시 미허용 등) 알림함 기록만 하고 조용히 끝난다 — 정상 케이스다.
	 * FCM 발송 자체가 실패해도 예외를 던지지 않는다: 호출자는 대부분
	 * {@code @TransactionalEventListener(AFTER_COMMIT)} 안에서 부르므로, 여기서 던져 봐야
	 * 이미 커밋된 구매·등록을 되돌릴 수 없다.
	 */
	void notify(Integer userId, NotificationType type, String title, String body, Integer refId);
}
