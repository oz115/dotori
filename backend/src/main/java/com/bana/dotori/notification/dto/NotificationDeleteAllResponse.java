package com.bana.dotori.notification.dto;

/** 알림함 "모두 삭제" 결과. 실제로 지워진 건수를 돌려준다 (0 건이어도 성공). */
public record NotificationDeleteAllResponse(
	int deletedCount
) {
}
