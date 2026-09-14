package com.bana.dotori.investment.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.bana.dotori.investment.dto.InvestmentProductSummary;
import com.bana.dotori.investment.entity.InvestmentProduct;

@Component
public class InvestmentProductMapper {

	/**
	 * currentPrice/previousPrice 는 가족별 가격 이력에서 고른 값 (이력이 없으면 시작가).
	 * 등락률은 전일가 대비 %, 소수 둘째 자리 반올림 — 명세 예시 4.35 형식.
	 */
	public InvestmentProductSummary toSummary(InvestmentProduct product, int currentPrice, int previousPrice) {
		return new InvestmentProductSummary(product.getId(), product.getName(), product.getCategory(),
			currentPrice, changeRate(currentPrice, previousPrice), product.getIsActive());
	}

	private double changeRate(int currentPrice, int previousPrice) {
		if (previousPrice <= 0) {
			return 0.0;
		}
		return BigDecimal.valueOf(currentPrice - previousPrice)
			.multiply(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(previousPrice), 2, RoundingMode.HALF_UP)
			.doubleValue();
	}
}
