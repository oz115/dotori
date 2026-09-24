package com.bana.dotori.investment.dto;

import java.util.List;

public record InvestmentTransactionListResponse(
	List<InvestmentTransactionSummary> transactions
) {
}
