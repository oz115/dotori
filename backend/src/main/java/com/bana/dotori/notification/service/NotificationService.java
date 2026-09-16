package com.bana.dotori.notification.service;

import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.entity.NotificationType;

public interface NotificationService {

	/**
	 * 로그인한 본인의 알림함을 조회한다 (NOTI-01). type/isRead 는 둘 다 선택 필터이며,
	 * 응답의 unreadCount 는 이 필터와 무관하게 항상 전체 미읽 개수다.
	 */
	NotificationListResponse getNotifications(Integer userId, NotificationType type, Boolean isRead, int page,
			int size);
}
