package com.bana.dotori.investment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투자 상품(가상 회사). 뉴스·목록 API 가 쓰는 컬럼까지 매핑했다 — news_context 등
 * 나머지 컬럼은 AI 서버 전용이라 매핑하지 않는다 (매수·매도 태스크에서 필요해지면 확장).
 *
 * current_price 는 시드로 넣는 "시작가"다. AI 가 매일 쓰는 실제 가격은 가족별
 * investment_price_history 에 쌓이고, 이력이 없는 가족은 이 시작가를 현재가로 본다.
 */
@Entity
@Table(name = "investment_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "investment_product_id")
	private Integer id;

	@Column(name = "name", length = 100, nullable = false)
	private String name;

	// ERD 상 NULL 허용 — 분류가 정해지지 않은 상품이 있을 수 있다
	@Enumerated(EnumType.STRING)
	@Column(name = "category")
	private InvestmentProductCategory category;

	@Column(name = "current_price", nullable = false)
	private Integer currentPrice;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Builder
	private InvestmentProduct(String name, InvestmentProductCategory category, Integer currentPrice,
		Boolean isActive) {
		this.name = name;
		this.category = category;
		this.currentPrice = currentPrice;
		this.isActive = isActive;
	}
}
