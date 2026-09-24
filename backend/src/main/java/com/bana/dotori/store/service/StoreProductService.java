package com.bana.dotori.store.service;

import com.bana.dotori.store.dto.StoreProductCreateRequest;
import com.bana.dotori.store.dto.StoreProductDetailResponse;
import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.dto.StoreProductUpdateRequest;
import com.bana.dotori.store.dto.StorePurchaseResponse;

public interface StoreProductService {

	StoreProductListResponse getProducts(Integer userId, Integer familyId);

	StoreProductDetailResponse getProduct(Integer userId, Integer familyId, Integer productId);

	StoreProductDetailResponse createProduct(Integer userId, Integer familyId, StoreProductCreateRequest request);

	StoreProductDetailResponse updateProduct(Integer userId, Integer familyId, Integer productId,
			StoreProductUpdateRequest request);

	void deleteProduct(Integer userId, Integer familyId, Integer productId);

	StorePurchaseResponse purchaseProduct(Integer userId, Integer familyId, Integer productId, int quantity);
}
