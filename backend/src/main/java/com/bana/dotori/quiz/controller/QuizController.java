package com.bana.dotori.quiz.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.quiz.dto.QuizHistoryResponse;
import com.bana.dotori.quiz.dto.QuizSubmitRequest;
import com.bana.dotori.quiz.dto.QuizSubmitResponse;
import com.bana.dotori.quiz.dto.QuizTodayResponse;
import com.bana.dotori.quiz.service.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 경제 단어장 퀴즈 API (QUIZ-01·02·03). 오늘의 퀴즈·제출은 아이 본인만, 이력은 부모도 본다.
 */
@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/quizzes")
@RequiredArgsConstructor
public class QuizController {

	private final QuizService quizService;

	@GetMapping("/today")
	public ResponseEntity<QuizTodayResponse> getTodayQuiz(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId) {
		return ResponseEntity.ok(quizService.getTodayQuiz(userId, familyId, childId));
	}

	@GetMapping("/history")
	public ResponseEntity<QuizHistoryResponse> getHistory(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ResponseEntity.ok(quizService.getHistory(userId, familyId, childId, startDate, endDate));
	}

	@PostMapping("/{quizId}/submit")
	public ResponseEntity<QuizSubmitResponse> submitQuiz(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer quizId,
		@Valid @RequestBody QuizSubmitRequest request) {
		return ResponseEntity.ok(quizService.submitQuiz(userId, familyId, childId, quizId, request));
	}
}
