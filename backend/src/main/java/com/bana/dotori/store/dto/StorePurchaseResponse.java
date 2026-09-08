package com.bana.dotori.store.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 구매 결과 (SHOP-06). Notion 응답 형식을 그대로 따른다.
 */
public record StorePurchaseResponse(
	Integer purchaseId,
	Integer storeProductId,
	String productName,
	int quantity,
	int unitPrice,
	int totalPrice,
	int pointBalanceAfter,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	OffsetDateTime purchasedAt
) {
}
