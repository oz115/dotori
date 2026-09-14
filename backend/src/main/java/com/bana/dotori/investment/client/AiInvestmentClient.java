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
}
