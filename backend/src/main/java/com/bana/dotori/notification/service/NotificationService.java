package com.bana.dotori.notification.service;

import com.bana.dotori.notification.dto.NotificationItem;
import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.dto.NotificationReadResponse;
import com.bana.dotori.notification.entity.NotificationType;

public interface NotificationService {

	/**
	 * 로그인한 본인의 알림함을 조회한다 (NOTI-01). type/isRead 는 둘 다 선택 필터이며,
	 * 응답의 unreadCount 는 이 필터와 무관하게 항상 전체 미읽 개수다.
	 */
	NotificationListResponse getNotifications(Integer userId, NotificationType type, Boolean isRead, int page,
			int size);

	/**
	 * 알림 하나의 상세를 조회한다 (NOTI-02). 존재하지 않으면 NOTIFICATION_NOT_FOUND,
	 * 존재하지만 본인 소유가 아니면 FORBIDDEN — 상점 도메인과 달리 존재 여부를 굳이 숨기지 않는다
	 * (명세가 두 상태를 명시적으로 구분).
	 */
	NotificationItem getNotification(Integer userId, Integer notificationId);

	/**
	 * 알림을 읽음 처리한다 (NOTI-03). 소유권 규칙은 getNotification과 같다. 이미 읽은
	 * 알림에 다시 호출해도 에러 없이 그대로 성공한다(멱등).
	 */
	NotificationReadResponse markAsRead(Integer userId, Integer notificationId);
}
