package com.bana.dotori.quiz.dto;

/**
 * 퀴즈 제출 결과 (QUIZ-03). {@code correctAnswer}·{@code explanation}은 정답이거나 이번이
 * 3번째(마지막) 시도일 때만 채워진다 — 그 전 오답에서는 둘 다 null (팀 결정, 답을 바로
 * 알려주지 않고 재도전을 유도한다).
 */
public record QuizSubmitResponse(
	Integer quizAttemptId,
	boolean isCorrect,
	String correctAnswer,
	String explanation,
	int earnedPoints,
	int pointBalanceAfter
) {
}
