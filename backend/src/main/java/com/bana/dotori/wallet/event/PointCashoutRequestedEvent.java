package com.bana.dotori.wallet.event;

/** 아이가 환전을 요청했다는 애플리케이션 이벤트 (WLT-04). 부모가 승인해야 실제 송금이 일어난다. */
public record PointCashoutRequestedEvent(
	Integer cashoutRequestId,
	Integer parentId,
	Integer childId,
	int pointAmount
) {
}
