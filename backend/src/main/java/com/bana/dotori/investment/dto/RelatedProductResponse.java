package com.bana.dotori.investment.dto;

import com.bana.dotori.investment.entity.InvestmentProductCategory;

/** 뉴스와 연결된 종목 요약. category 는 분류 미정 상품이면 null 이다. */
public record RelatedProductResponse(
	Integer investmentProductId,
	String productName,
	InvestmentProductCategory category
) {
}
