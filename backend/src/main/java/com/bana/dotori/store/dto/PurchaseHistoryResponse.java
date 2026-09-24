package com.bana.dotori.store.dto;

import java.util.List;

public record PurchaseHistoryResponse(
	List<PurchaseHistoryItem> purchases
) {
}
