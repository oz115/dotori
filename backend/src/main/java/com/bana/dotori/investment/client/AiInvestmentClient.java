package com.bana.dotori.investment.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

/**
 * AI 서버(FastAPI) 투자 내부 API 호출. 뉴스 생성(NEWS-02)·가격 반영(NEWS-03)의 실제
 * 구현은 AI/app/domains/investment 에 있고, 백엔드는 트리거만 한다.
 */
@Component
@RequiredArgsConstructor
public class AiInvestmentClient {

	private final RestClient aiRestClient;

	/** 오늘의 뉴스 생성 + 가족별 가격 갱신. AI 쪽에서 멱등이라 재호출해도 안전하다. */
	public AiDailyInvestmentResponse runDailyUpdate() {
		return aiRestClient.post()
			.uri("/internal/investment/daily")
			.retrieve()
			.body(AiDailyInvestmentResponse.class);
	}

	/**
	 * 막 온보딩한 가족의 가격 이력 즉시 생성 — 다음 크론을 기다리지 않고 가입 당일에
	 * 차트에 일주일치 흐름이 보이게 한다. AI 쪽에서 멱등이라(이미 이력이 있으면 0)
	 * 재호출해도 안전하다.
	 */
	public AiPriceBackfillResponse backfillFamilyPrices(Integer familyId) {
		return aiRestClient.post()
			.uri("/internal/investment/families/{familyId}/price-backfill", familyId)
			.retrieve()
			.body(AiPriceBackfillResponse.class);
	}
}
