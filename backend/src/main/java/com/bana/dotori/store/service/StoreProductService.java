package com.bana.dotori.store.service;

import com.bana.dotori.store.dto.StoreProductListResponse;

public interface StoreProductService {

	StoreProductListResponse getProducts(Integer userId, Integer familyId);
}
