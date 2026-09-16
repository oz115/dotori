package com.bana.dotori.investment.service;

import java.time.LocalDate;

import com.bana.dotori.investment.dto.InvestmentHoldingsResponse;
import com.bana.dotori.investment.dto.InvestmentPriceHistoryResponse;
import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
import com.bana.dotori.investment.dto.InvestmentProductListResponse;
import com.bana.dotori.investment.dto.InvestmentTransactionListResponse;
import com.bana.dotori.investment.entity.InvestmentProductCategory;
import com.bana.dotori.investment.entity.InvestmentTransactionType;

public interface InvestmentProductService {

	InvestmentProductListResponse getProducts(Integer userId, InvestmentProductCategory category);

	InvestmentHoldingsResponse getHoldings(Integer userId, Integer childId);

	InvestmentProductDetailResponse getProductDetail(Integer userId, Integer productId);

	InvestmentPriceHistoryResponse getPriceHistory(Integer userId, Integer productId, LocalDate startDate,
		LocalDate endDate);

	InvestmentTransactionListResponse getTransactions(Integer userId, Integer childId, LocalDate startDate,
		LocalDate endDate, InvestmentTransactionType type);
}
