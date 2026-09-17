package com.bana.dotori.investment.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가족별 투자 상품 가격 이력. AI 서버가 매일 00:05(KST) 뉴스 반영 때 가족×상품마다
 * 한 행씩 쓴다 — 백엔드는 읽기만 한다. (가족, 상품)의 최신 행이 그 가족의 현재가다.
 */
@Entity
@Table(name = "investment_price_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentPriceHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "investment_price_history_id")
	private Integer id;

	@Column(name = "family_id", nullable = false)
	private Integer familyId;

	@Column(name = "investment_product_id", nullable = false)
	private Integer investmentProductId;

	@Column(name = "price", nullable = false)
	private Integer price;

	@Column(name = "recorded_at", nullable = false)
	private OffsetDateTime recordedAt;

	@Builder
	private InvestmentPriceHistory(Integer familyId, Integer investmentProductId, Integer price,
		OffsetDateTime recordedAt) {
		this.familyId = familyId;
		this.investmentProductId = investmentProductId;
		this.price = price;
		this.recordedAt = recordedAt;
	}
}
