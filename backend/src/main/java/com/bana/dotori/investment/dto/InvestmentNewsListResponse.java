package com.bana.dotori.investment.dto;

import java.util.List;

public record InvestmentNewsListResponse(
	List<InvestmentNewsSummary> news
) {
}
