package com.bana.dotori.quiz.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.RequiredArgsConstructor;

/**
 * AI 서버(FastAPI) 퀴즈 생성 내부 API 호출. 실제 생성·배정 로직은
 * AI/app/domains/quiz 에 있고, 백엔드는 트리거만 한다.
 *
 * <p>{@code aiRestClient} 빈은 investment 도메인의 {@code AiClientConfig}가 등록한 것을
 * 그대로 재사용한다 — base URL과 X-Internal-Token 헤더는 AI 서버 전체에 공통이라
 * 도메인마다 새로 만들 이유가 없다.
 */
@Component
@RequiredArgsConstructor
public class AiQuizClient {

	private final RestClient aiRestClient;

	/** 오늘자 퀴즈 생성 + 가족 배정. AI 쪽에서 (child_id, quiz_date) 존재 여부로 멱등이다. */
	public AiQuizGenerateResponse generateToday() {
		return aiRestClient.post()
			.uri("/internal/quiz/generate")
			.retrieve()
			.body(AiQuizGenerateResponse.class);
	}

	/**
	 * 가족 하나에만 오늘자 퀴즈를 배정한다. 자정 크론이 돌 뒤에 자녀가 합류했을 때 쓴다.
	 *
	 * <p>형제가 이미 오늘 문제를 받았으면 AI 가 그 문제를 그대로 복사한다 — 한 가족은 같은 문제를
	 * 푸는 것이 배정 규칙이고, 그 경우 LLM 호출도 나가지 않는다. 재호출은 안전하다(멱등).
	 */
	public AiQuizAssignResponse assignFamily(Integer familyId) {
		return aiRestClient.post()
			.uri("/internal/quiz/assign")
			.body(new AiQuizAssignRequest(familyId))
			.retrieve()
			.body(AiQuizAssignResponse.class);
	}

	/** AI 서버가 받는 본문. snake_case 라 필드명을 그대로 맞춘다. */
	private record AiQuizAssignRequest(@JsonProperty("family_id") Integer familyId) {
	}
}
