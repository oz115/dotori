package com.bana.dotori.store.dto;

import com.bana.dotori.store.entity.StoreProductCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 상품 부분 수정 요청 (SHOP-04). 값이 있는 필드만 반영한다(partial update).
 *
 * <p>value 는 요청에 없다 — AI 서버만 채울 수 있다. price 는 원래 AI 산정값이지만
 * 부모가 직접 확인해서 덮어쓸 수도 있게 열어 둔다.
 *
 * <p>name/description/category 가 바뀌어도 기존 value/price 는 건드리지 않는다.
 * AI 가 이미 산정한 값을 무효화하는 재평가 로직은 아직 없다(추후 별도 작업).
 */
public record StoreProductUpdateRequest(
	@Size(max = 100) String name,
	@Size(max = 255) String description,
	StoreProductCategory category,
	@Positive Integer price,
	@Min(0) Integer quantity
) {
}
