package com.bana.dotori.investment.entity;

/**
 * 투자 상품 분류. DDL 의 네이티브 ENUM(investment_products_category) 라벨과
 * 상수 이름이 정확히 같아야 한다.
 */
public enum InvestmentProductCategory {
	STOCK,
	BOND,
	ETF,
	FUND,
	OTHER
}
