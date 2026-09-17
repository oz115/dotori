package com.bana.dotori.savings.event;

/**
 * 예적금이 만기에 도달했다는 애플리케이션 이벤트.
 *
 * <p>만기가 되어도 돈은 저절로 들어오지 않는다 — 아이가 수령(SAV-05)을 눌러야 원금과
 * 이자가 지갑으로 간다. 알리지 않으면 만기된 돈이 그대로 방치된다.
 *
 * @param payout 지금 수령하면 받게 되는 금액 (원금 + 쌓인 이자)
 */
public record SavingsMaturedEvent(
	Integer savingsSubscriptionId,
	Integer childId,
	String productName,
	int payout
) {
}
