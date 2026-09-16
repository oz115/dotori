package com.bana.dotori.notification.entity;

/**
 * notifications.type. DB 의 notifications_type enum 과 값이 1:1 대응해야 한다.
 */
public enum NotificationType {
	INVITATION,
	PURCHASE,
	CHARGE,
	CASHOUT,
	CHORE,
	SAVINGS,
	INVESTMENT
}
