package com.bana.dotori.investment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 뉴스-종목 연결. 테이블은 N:N 조인 형태지만 **현재 설계상 1:1 로만 쓰인다** —
 * 노션 「투자」가 1 경제요인 : 1 회사로 정해 뉴스 하나가 가리키는 회사도 하나고,
 * 생성 주체인 AI 서버(AI/app/domains/investment)도 뉴스당 한 행만 넣는다.
 * 조회 응답의 relatedProducts 가 배열인 것은 테이블 형태를 따른 것일 뿐,
 * 지금은 항상 원소가 1개다.
 */
@Entity
@Table(name = "investment_news_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentNewsProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "investment_news_product_id")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "investment_news_id", nullable = false)
	private InvestmentNews news;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "investment_product_id", nullable = false)
	private InvestmentProduct product;

	@Builder
	private InvestmentNewsProduct(InvestmentNews news, InvestmentProduct product) {
		this.news = news;
		this.product = product;
	}
}
