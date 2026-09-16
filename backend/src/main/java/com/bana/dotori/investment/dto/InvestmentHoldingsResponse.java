package com.bana.dotori.investment.dto;

import java.util.List;

public record InvestmentHoldingsResponse(
	List<InvestmentHoldingSummary> holdings
) {
}
