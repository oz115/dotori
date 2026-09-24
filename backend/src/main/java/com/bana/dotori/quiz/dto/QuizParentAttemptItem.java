package com.bana.dotori.quiz.dto;

import java.time.OffsetDateTime;

/**
 * 부모용 오늘의 퀴즈 확인({@link QuizParentTodayResponse})의 시도 1회. 아이가 실제로 제출한 답과
 * 그 정오답을 그대로 보여준다.
 */
public record QuizParentAttemptItem(
	Integer quizAttemptId,
	int attemptNumber,
	String submittedAnswer,
	boolean isCorrect,
	int earnedPoints,
	OffsetDateTime submittedAt
) {
}
