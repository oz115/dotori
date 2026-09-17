package com.bana.dotori.notification.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.notification.entity.NotificationType;

/**
 * isRead 를 컴포넌트명 그대로 쓴다. record 는 Jackson이 컴포넌트 이름을 그대로 JSON 키로
 * 쓰므로(POJO의 is-접두사 getter 벗기기 규칙이 적용되지 않는다), 명세의 "isRead" 키와
 * 정확히 맞아떨어진다.
 */
public record NotificationItem(
	Integer notificationId,
	NotificationType type,
	String title,
	String body,
	Integer refId,
	boolean isRead,
	OffsetDateTime createdAt
) {
}
