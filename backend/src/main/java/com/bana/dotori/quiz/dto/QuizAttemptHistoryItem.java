package com.bana.dotori.quiz.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record QuizAttemptHistoryItem(
	Integer quizAttemptId,
	LocalDate quizDate,
	String question,
	String submittedAnswer,
	boolean isCorrect,
	int earnedPoints,
	OffsetDateTime submittedAt
) {
}
