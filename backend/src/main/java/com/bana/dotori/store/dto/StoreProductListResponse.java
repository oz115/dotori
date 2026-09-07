package com.bana.dotori.store.dto;

import java.util.List;

public record StoreProductListResponse(
	List<StoreProductSummary> products
) {
}
