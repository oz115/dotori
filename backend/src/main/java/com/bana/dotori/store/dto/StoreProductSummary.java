package com.bana.dotori.store.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.store.entity.StoreProductCategory;

/**
 * 목록의 상품 한 건 (SHOP-01).
 *
 * @param price 포인트 가격. AI 가치 산정이 끝나기 전에는 null (= 가격 산정 중)
 */
public record StoreProductSummary(
	Integer storeProductId,
	String name,
	StoreProductCategory category,
	Integer price,
	int quantity,
	OffsetDateTime createdAt
) {
}
