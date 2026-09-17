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
	INVESTMENT,
	/** 하루 시작 알림 — 오늘의 퀴즈·뉴스가 준비됐음을 아침에 한 번 알린다 */
	DAILY
}
