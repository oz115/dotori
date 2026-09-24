package com.bana.dotori.investment.entity;

/**
 * 뉴스가 관련 종목 가격에 주는 영향 방향.
 *
 * <p>DDL(01-schema.sql)의 네이티브 ENUM(investment_news_impact) 라벨과 상수 이름이
 * 정확히 같아야 한다 — users.role 과 같은 이유로, stringtype=unspecified 덕에
 * {@code @Enumerated(EnumType.STRING)} 매핑이 그대로 통한다.
 */
public enum InvestmentNewsImpact {
	POSITIVE,
	NEGATIVE
}
