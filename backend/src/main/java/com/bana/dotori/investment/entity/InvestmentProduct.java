package com.bana.dotori.investment.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투자 상품(가상 회사). 가격 조회와 뉴스 복습에 필요한 기존 컬럼을 매핑한다.
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

	// ERD 상 NULL 허용 — 설명이 없는 상품이 있을 수 있다
	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "news_context", nullable = false, insertable = false, updatable = false)
	private String newsContext;

	// ERD 상 NULL 허용 — 분류가 정해지지 않은 상품이 있을 수 있다
	@Enumerated(EnumType.STRING)
	@Column(name = "category")
	private InvestmentProductCategory category;

	@Column(name = "current_price", nullable = false)
	private Integer currentPrice;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	// 가격 이력이 없는 가족에게 "가격 갱신 일시"로 내려 줄 대체값 (시드 시각)
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Builder
	private InvestmentProduct(String name, String description, InvestmentProductCategory category,
		Integer currentPrice, Boolean isActive, OffsetDateTime updatedAt) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.currentPrice = currentPrice;
		this.isActive = isActive;
		this.updatedAt = updatedAt;
	}
}
