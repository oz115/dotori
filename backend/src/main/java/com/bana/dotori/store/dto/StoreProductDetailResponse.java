package com.bana.dotori.store.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.bana.dotori.store.entity.StoreProductCategory;

/**
 * 상품 상세 (SHOP-02).
 *
 * @param value AI 가 비동기로 산정하는 가치 가중치(아이 하루 소득 D의 배수). 산정 전에는 null
 * @param price 포인트 가격. value 산정이 끝나야 계산되므로 산정 전에는 null (= 가격 산정 중)
 */
public record StoreProductDetailResponse(
	Integer storeProductId,
	String name,
	String description,
	StoreProductCategory category,
	BigDecimal value,
	Integer price,
	int quantity,
	Integer createdBy,
	OffsetDateTime createdAt
) {
}
