package com.bana.dotori.store.event;

/**
 * 상품 구매가 확정됐다는 애플리케이션 이벤트.
 *
 * <p>서비스 트랜잭션 안에서 이 이벤트만 발행하고, 실제 알림 발송은
 * {@link StorePurchaseNotificationListener} 가 커밋 이후에 한다. 트랜잭션이 롤백되면
 * 애초에 리스너가 호출되지 않으므로, 되돌려진 구매에 대해 알림이 나가는 사고를 막는다.
 */
public record StoreProductPurchasedEvent(
	Integer purchaseId,
	Integer storeProductId,
	Integer parentUserId,
	String productName,
	int quantity,
	int totalPrice
) {
}
