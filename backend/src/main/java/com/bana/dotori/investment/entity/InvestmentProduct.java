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
 * 투자 상품(가상 회사). 지금은 뉴스 API 가 관련 종목의 이름·분류를 내려 주는 데 필요한
 * 컬럼만 매핑했다 — 상품 목록·매수·매도 등 투자 API 태스크에서 나머지 컬럼을 확장한다.
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

	@Builder
	private InvestmentProduct(String name, InvestmentProductCategory category) {
		this.name = name;
		this.category = category;
	}
}
