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

	/**
	 * 알림함에는 남기되 푸시는 보내지 않는다.
	 *
	 * <p>사람이 한 일에 대한 응답이 아니라 서버가 스스로 정리한 일을 알릴 때 쓴다. 그런 일은
	 * 크론이 자정에 처리하는데, 그때 푸시를 보내면 자는 아이 폰이 새벽에 울린다. 알림함에만
	 * 남겨 두면 다음에 앱을 열었을 때 왜 그렇게 됐는지 읽을 수 있다.
	 */
	void record(Integer userId, NotificationType type, String title, String body, Integer refId);
}
