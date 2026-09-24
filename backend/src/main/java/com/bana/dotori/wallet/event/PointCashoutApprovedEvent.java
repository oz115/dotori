package com.bana.dotori.wallet.event;

/**
 * 부모가 환전을 승인하고 송금까지 끝났다는 애플리케이션 이벤트 (WLT-05).
 *
 * <p>송금이 롤백되면 리스너가 호출되지 않는다. 돈이 안 갔는데 "보냈어요" 가 나가면 안 된다.
 */
public record PointCashoutApprovedEvent(
	Integer cashoutRequestId,
	Integer childId,
	int pointAmount
) {
}
