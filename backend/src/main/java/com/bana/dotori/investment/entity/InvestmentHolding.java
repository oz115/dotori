package com.bana.dotori.investment.entity;

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
 * 아이별 현재 보유 현황 — (child_id, investment_product_id)당 한 행 (부분 유니크).
 * 매수/매도 API 가 거래 기록(investment_transactions)과 같은 트랜잭션으로 갱신하고,
 * 보유 조회는 이 테이블만 읽는다. average_buy_price 는 매수 시점 가격의 수량 가중 평균
 * (한 번만 샀으면 그때 가격 그대로) — 전량 매도하면 행을 지운다.
 */
@Entity
@Table(name = "investment_holdings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentHolding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "investment_holding_id")
	private Integer id;

	@Column(name = "family_id", nullable = false)
	private Integer familyId;

	@Column(name = "child_id", nullable = false)
	private Integer childId;

	@Column(name = "investment_product_id", nullable = false)
	private Integer investmentProductId;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "average_buy_price", nullable = false)
	private Integer averageBuyPrice;

	@Builder
	private InvestmentHolding(Integer familyId, Integer childId, Integer investmentProductId, Integer quantity,
		Integer averageBuyPrice) {
		this.familyId = familyId;
		this.childId = childId;
		this.investmentProductId = investmentProductId;
		this.quantity = quantity;
		this.averageBuyPrice = averageBuyPrice;
	}
}
