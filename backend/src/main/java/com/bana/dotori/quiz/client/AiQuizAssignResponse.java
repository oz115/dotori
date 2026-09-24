package com.bana.dotori.quiz.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 POST /internal/quiz/assign 응답. 로그용 요약값만 받는다.
 *
 * @param familyId           배정 대상 가족
 * @param tier               그 가족의 financial_knowledge_level. 배정할 자녀가 없으면 null
 * @param poolSize           이번에 만든(또는 형제에게서 가져온) 문제 수
 * @param inserted           실제로 새로 삽입된 quizzes 행 수
 * @param skipped            이미 오늘자 퀴즈가 있어 건너뛴 자녀 수
 * @param failedFamilies     삽입 중 예외가 나서 실패한 가족 수 (0 또는 1)
 * @param reusedSiblingQuiz  형제가 이미 받은 오늘 문제를 그대로 썼는지 (true 면 LLM 호출 없음)
 */
public record AiQuizAssignResponse(
	@JsonProperty("family_id") int familyId,
	String tier,
	@JsonProperty("pool_size") int poolSize,
	int inserted,
	int skipped,
	@JsonProperty("failed_families") int failedFamilies,
	@JsonProperty("reused_sibling_quiz") boolean reusedSiblingQuiz
) {
}
