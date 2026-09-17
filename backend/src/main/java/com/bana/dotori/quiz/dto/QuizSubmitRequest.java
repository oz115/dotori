package com.bana.dotori.quiz.dto;

import jakarta.validation.constraints.NotBlank;

public record QuizSubmitRequest(
	@NotBlank(message = "답안을 입력해 주세요")
	String answer
) {
}
