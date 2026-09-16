package com.bana.dotori.investment.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.investment.dto.InvestmentHoldingSummary;
import com.bana.dotori.investment.dto.InvestmentOrderResponse;
import com.bana.dotori.investment.dto.InvestmentPriceHistoryResponse;
import com.bana.dotori.investment.dto.InvestmentPricePoint;
import com.bana.dotori.investment.dto.InvestmentProductDetailResponse;
import com.bana.dotori.investment.dto.InvestmentProductSummary;
import com.bana.dotori.investment.entity.InvestmentPriceHistory;
import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentTransaction;

@Component
public class InvestmentProductMapper {

	// 명세의 가격 갱신 일시 예시가 +09:00 — DB 의 TIMESTAMPTZ 를 한국 시간으로 바꿔 내려 준다
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/**
	 * currentPrice/previousPrice 는 가족별 가격 이력에서 고른 값 (이력이 없으면 시작가).
	 * 등락은 전일가 대비 포인트 차이 — 1000 → 1100 이면 +100 (등락률 %는 쓰지 않기로 한 팀 결정).
	 */
	public InvestmentProductSummary toSummary(InvestmentProduct product, int currentPrice, int previousPrice) {
		return new InvestmentProductSummary(product.getId(), product.getName(), product.getCategory(),
			currentPrice, currentPrice - previousPrice, product.getIsActive());
	}

	/** updatedAt 은 가격 갱신 일시 — 최신 가격 행의 기록 시각, 이력이 없으면 상품 시드 시각. */
	public InvestmentProductDetailResponse toDetailResponse(InvestmentProduct product, int currentPrice,
		int previousPrice, OffsetDateTime updatedAt) {
		return new InvestmentProductDetailResponse(product.getId(), product.getName(), product.getDescription(),
			product.getCategory(), currentPrice, currentPrice - previousPrice, previousPrice,
			product.getIsActive(), updatedAt.atZoneSameInstant(KST).toOffsetDateTime());
	}

	/** 보유 종목 한 건 — 수량·매수 가격은 investment_holdings 행 그대로, currentPrice 는 가족별 최신가. */
	public InvestmentHoldingSummary toHoldingSummary(InvestmentProduct product, int quantity,
		int buyPrice, int currentPrice) {
		return new InvestmentHoldingSummary(product.getId(), product.getName(), quantity, buyPrice,
			currentPrice);
	}

	/** 매수/매도 체결 결과 — 남긴 거래 행 + 지갑이 계산해 준 거래 후 잔액. */
	public InvestmentOrderResponse toOrderResponse(InvestmentTransaction transaction, int pointBalanceAfter) {
		return new InvestmentOrderResponse(transaction.getId(), transaction.getTransactionType(),
			transaction.getQuantity(), transaction.getUnitPrice(), transaction.getTotalAmount(),
			pointBalanceAfter, transaction.getCreatedAt().atZoneSameInstant(KST).toOffsetDateTime());
	}

	/** 가격 이력 (차트용) — 기록 일시를 한국 시간으로 바꿔 시간 오름차순 그대로 내려 준다. */
	public InvestmentPriceHistoryResponse toPriceHistoryResponse(Integer productId,
		List<InvestmentPriceHistory> rows) {
		return new InvestmentPriceHistoryResponse(productId, rows.stream()
			.map(row -> new InvestmentPricePoint(row.getPrice(),
				row.getRecordedAt().atZoneSameInstant(KST).toOffsetDateTime()))
			.toList());
	}
}
