package com.bana.dotori.investment.service;

import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.entity.InvestmentProductCategory;

public interface InvestmentProductService {

	InvestmentProductListResponse getProducts(Integer userId, InvestmentProductCategory category);

	InvestmentProductDetailResponse getProductDetail(Integer userId, Integer productId);
}
