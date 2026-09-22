package com.bana.dotori.store.event;

import com.bana.dotori.store.entity.StoreProductCategory;

/**
 * 상품 정보(이름·설명·카테고리)가 바뀌어 AI 가치 재산정이 필요하다는 애플리케이션 이벤트 (SHOP-04).
 *
 * <p>{@link StoreProductCreatedEvent} 와 페이로드는 같지만 타입을 따로 둔다 — 등록 이벤트는
 * 자녀에게 "새 상품 등록" 알림까지 발송하는데({@code StoreProductRegisteredNotificationListener}),
 * 단순 수정 때 그 알림이 나가면 안 되기 때문이다. 이 이벤트는 가치 산정 큐 발행
 * ({@link StoreProductValueEvaluationListener})에만 쓰인다.
 *
 * <p>재산정은 서비스가 {@code value/price/reason} 을 NULL 로 되돌린 뒤 이 이벤트를 발행해
 * 시작된다 — AI 컨슈머가 {@code value IS NULL} 일 때만 다시 산정하기 때문이다.
 */
public record StoreProductReevaluationRequestedEvent(
	Integer storeProductId,
	Integer familyId,
	String name,
	String description,
	StoreProductCategory category
) {
}
