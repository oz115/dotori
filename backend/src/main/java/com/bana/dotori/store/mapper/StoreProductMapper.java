package com.bana.dotori.store.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.store.dto.StoreProductDetailResponse;
import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.dto.StoreProductSummary;
import com.bana.dotori.store.entity.StoreProduct;

@Component
public class StoreProductMapper {

	public StoreProductListResponse toListResponse(List<StoreProduct> products) {
		return new StoreProductListResponse(products.stream()
			.map(this::toSummary)
			.toList());
	}

	public StoreProductSummary toSummary(StoreProduct product) {
		return new StoreProductSummary(product.getId(), product.getName(), product.getDescription(),
			product.getCategory(), product.getPrice(), product.getQuantity(), product.getCreatedAt());
	}

	public StoreProductDetailResponse toDetailResponse(StoreProduct product) {
		return new StoreProductDetailResponse(product.getId(), product.getName(), product.getDescription(),
			product.getCategory(), product.getValue(), product.getPrice(), product.getReason(),
			product.getQuantity(), product.getCreatedBy(), product.getCreatedAt());
	}
}
