package com.bana.dotori.quiz.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 POST /internal/quiz/generate 응답. 로그용 요약값만 받는다.
 *
 * @param poolSize       이번에 LLM이 생성한 문제 풀 크기 (티어당 3개, 보통 9)
 * @param inserted       실제로 새로 삽입된 quizzes 행 수
 * @param skipped        이미 오늘자 퀴즈가 있어 건너뛴 자녀 수
 * @param failedFamilies 삽입 중 예외가 나서 실패한 가족 수
 */
public record AiQuizGenerateResponse(
	@JsonProperty("pool_size") int poolSize,
	int inserted,
	int skipped,
	@JsonProperty("failed_families") int failedFamilies
) {
}
