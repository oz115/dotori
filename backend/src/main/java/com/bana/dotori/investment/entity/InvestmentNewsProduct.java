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
 * 뉴스-종목 연결. 한 뉴스가 여러 종목의 가격에 영향을 줄 수 있어 매핑 테이블로 잇는다.
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
