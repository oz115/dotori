package com.bana.dotori.store.event;

import com.bana.dotori.store.entity.StoreProductCategory;

/**
 * 상품이 새로 등록됐다는 애플리케이션 이벤트.
 *
 * <p>서비스 트랜잭션 안에서 이 이벤트만 발행하고, 실제 RabbitMQ 발송은
 * {@link StoreProductValueEvaluationListener} 가 커밋 이후에 한다.
 * 트랜잭션이 롤백되면 애초에 리스너가 호출되지 않으므로, 저장되지 않은 상품에 대해
 * AI 서버로 메시지가 나가는 사고를 막는다.
 */
public record StoreProductCreatedEvent(
	Integer storeProductId,
	Integer familyId,
	String name,
	String description,
	StoreProductCategory category
) {
}
