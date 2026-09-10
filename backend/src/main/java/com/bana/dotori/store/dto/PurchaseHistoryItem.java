package com.bana.dotori.store.dto;

import java.time.OffsetDateTime;

public record PurchaseHistoryItem(
	Integer purchaseId,
	String productName,
	int quantity,
	int totalPrice,
	OffsetDateTime purchasedAt
) {
}
