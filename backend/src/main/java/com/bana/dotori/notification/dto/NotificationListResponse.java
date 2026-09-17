package com.bana.dotori.notification.dto;

import java.util.List;

/**
 * unreadCount 는 이번 응답의 type/isRead 필터와 무관하게 그 사용자의 전체 미읽 개수다.
 * 알림 벨 배지 용도라 필터를 바꿔도 배지 숫자가 흔들리면 안 된다.
 */
public record NotificationListResponse(
	List<NotificationItem> notifications,
	int page,
	int totalPages,
	long unreadCount
) {
}
