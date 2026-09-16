package com.bana.dotori.investment.dto;

import java.time.OffsetDateTime;

/** 가격 이력의 한 점 — 차트의 x(기록 일시)·y(가격)다. */
public record InvestmentPricePoint(
	Integer price,
	OffsetDateTime recordedAt
) {
}
