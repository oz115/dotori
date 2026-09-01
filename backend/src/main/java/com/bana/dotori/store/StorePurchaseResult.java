package com.bana.dotori.store;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * SHOP-06 구매 결과. API 명세의 응답 형식을 그대로 따른다.
 *
 * <pre>
 * {
 *   "purchaseId": 1,
 *   "storeProductId": 1,
 *   "productName": "게임 30분 이용권",
 *   "quantity": 1,
 *   "unitPrice": 3000,
 *   "totalPrice": 3000,
 *   "pointBalanceAfter": 17000,
 *   "purchasedAt": "2026-08-28T14:00:00+09:00"
 * }
 * </pre>
 */
public record StorePurchaseResult(
		Integer purchaseId,
		Integer storeProductId,
		String productName,
		int quantity,
		int unitPrice,
		int totalPrice,
		int pointBalanceAfter,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
		OffsetDateTime purchasedAt) {
}
