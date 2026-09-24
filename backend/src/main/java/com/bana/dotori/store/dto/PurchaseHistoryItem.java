package com.bana.dotori.store.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.store.entity.StoreProductCategory;

/**
 * @param category 지금 상품의 카테고리. 상품이 삭제됐으면 null(카테고리 조회 불가) — 화면에서
 *                 "기타"로 대신 보여준다.
 */
public record PurchaseHistoryItem(
	Integer purchaseId,
	Integer storeProductId,
	String productName,
	StoreProductCategory category,
	int quantity,
	int totalPrice,
	OffsetDateTime purchasedAt
) {
}
