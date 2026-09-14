package com.bana.dotori.investment.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.investment.dto.InvestmentProductSummary;
import com.bana.dotori.investment.entity.InvestmentProduct;

@Component
public class InvestmentProductMapper {

	/**
	 * currentPrice/previousPrice 는 가족별 가격 이력에서 고른 값 (이력이 없으면 시작가).
	 * 등락은 전일가 대비 포인트 차이 — 1000 → 1100 이면 +100 (등락률 %는 쓰지 않기로 한 팀 결정).
	 */
	public InvestmentProductSummary toSummary(InvestmentProduct product, int currentPrice, int previousPrice) {
		return new InvestmentProductSummary(product.getId(), product.getName(), product.getCategory(),
			currentPrice, currentPrice - previousPrice, product.getIsActive());
	}
}
