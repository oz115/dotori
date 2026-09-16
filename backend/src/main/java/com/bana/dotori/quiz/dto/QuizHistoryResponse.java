package com.bana.dotori.quiz.dto;

import java.util.List;

public record QuizHistoryResponse(
	List<QuizAttemptHistoryItem> attempts
) {
}
