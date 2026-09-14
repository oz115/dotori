package com.bana.dotori.investment.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.bana.dotori.investment.entity.InvestmentNewsImpact;

/** 뉴스 목록의 한 건 — 본문(content)은 상세 조회에서만 내려 준다. */
public record InvestmentNewsSummary(
	Integer investmentNewsId,
	String title,
	InvestmentNewsImpact impact,
	List<RelatedProductResponse> relatedProducts,
	OffsetDateTime publishedAt
) {
}
