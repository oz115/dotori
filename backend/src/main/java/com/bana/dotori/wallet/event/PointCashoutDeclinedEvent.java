package com.bana.dotori.wallet.event;

/**
 * 부모가 환전을 거절했다는 애플리케이션 이벤트 (WLT-06).
 *
 * <p>요청 시점에 이미 빠져나갔던 도토리는 거절로 되돌아온다. 아이 입장에서는 잔액이
 * 다시 늘어나는 일이라 알려 줘야 한다.
 */
public record PointCashoutDeclinedEvent(
	Integer cashoutRequestId,
	Integer childId,
	int pointAmount
) {
}
