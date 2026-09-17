package com.bana.dotori.wallet.event;

/**
 * 아이가 포인트를 충전했다는 애플리케이션 이벤트 (WLT-01).
 *
 * <p>아이가 실제 금액을 부모 계좌로 보내고 같은 금액을 포인트로 받는 거래다. 부모 계좌에
 * 돈이 들어오는 일이라 부모가 알아야 한다.
 *
 * @param cashAmount 부모 계좌로 들어간 실제 금액(원)
 */
public record PointChargedEvent(
	Integer familyId,
	Integer childId,
	int cashAmount
) {
}
