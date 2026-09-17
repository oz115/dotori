package com.bana.dotori.investment.dto;

import java.util.List;

public record InvestmentProductListResponse(
	List<InvestmentProductSummary> products
) {
}
