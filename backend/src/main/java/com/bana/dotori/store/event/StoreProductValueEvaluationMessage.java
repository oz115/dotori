package com.bana.dotori.store.event;

import com.bana.dotori.store.entity.StoreProductCategory;

/**
 * AI 가치 산정 큐로 보내는 메시지 페이로드 (SHOP-03 등록 직후).
 *
 * <p>AI 서버는 이 정보를 근거로 value(D 에 곱해지는 가치 가중치)를 산정해 채운다.
 */
public record StoreProductValueEvaluationMessage(
	Integer storeProductId,
	Integer familyId,
	String name,
	String description,
	StoreProductCategory category
) {
}
