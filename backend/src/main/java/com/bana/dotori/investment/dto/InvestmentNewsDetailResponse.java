package com.bana.dotori.investment.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.bana.dotori.investment.entity.InvestmentNewsImpact;

public record InvestmentNewsDetailResponse(
	Integer investmentNewsId,
	String title,
	String content,
	InvestmentNewsImpact impact,
	List<RelatedProductResponse> relatedProducts,
	OffsetDateTime publishedAt
) {
}
