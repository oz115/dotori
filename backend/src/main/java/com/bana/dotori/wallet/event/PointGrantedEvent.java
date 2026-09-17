package com.bana.dotori.wallet.event;

/** 부모가 아이에게 도토리를 지급했다는 애플리케이션 이벤트 (WLT-03). */
public record PointGrantedEvent(
	Integer childId,
	int amount
) {
}
