package com.bana.dotori.store.dto;

import com.bana.dotori.store.entity.StoreProductCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 상품 생성 요청 (SHOP-03).
 *
 * <p>price 는 요청에 없다. AI 서버가 카테고리·설명을 근거로 가치(value)를 비동기 산정하고,
 * price = value * D 로 계산되기 때문이다 — 부모가 가격을 직접 정하게 하면 비현실적인 가격을 불러
 * 보상 체계를 무력화할 수 있어 의도적으로 막았다.
 */
public record StoreProductCreateRequest(
	@NotBlank @Size(max = 100) String name,
	@Size(max = 255) String description,
	@NotNull StoreProductCategory category,
	@NotNull @Min(0) Integer quantity
) {
}
