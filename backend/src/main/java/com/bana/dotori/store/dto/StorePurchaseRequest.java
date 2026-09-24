package com.bana.dotori.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 구매 요청 (SHOP-06). storeProductId 는 URL 경로에 이미 있어 요청 바디에서는 뺐다.
 */
public record StorePurchaseRequest(
	@NotNull @Min(1) Integer quantity
) {
}
