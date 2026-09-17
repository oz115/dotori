package com.bana.dotori.notification.dto;

public record NotificationReadResponse(
	Integer notificationId,
	boolean isRead
) {
}
