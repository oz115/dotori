package com.bana.dotori.quiz.service;

import java.time.LocalDate;

import com.bana.dotori.quiz.dto.QuizHistoryResponse;
import com.bana.dotori.quiz.dto.QuizParentTodayResponse;
import com.bana.dotori.quiz.dto.QuizSubmitRequest;
import com.bana.dotori.quiz.dto.QuizSubmitResponse;
import com.bana.dotori.quiz.dto.QuizTodayResponse;

public interface QuizService {

	QuizTodayResponse getTodayQuiz(Integer userId, Integer familyId, Integer childId);

	QuizParentTodayResponse getTodayQuizForParent(Integer userId, Integer familyId, Integer childId);

	QuizSubmitResponse submitQuiz(Integer userId, Integer familyId, Integer childId, Integer quizId,
		QuizSubmitRequest request);

	QuizHistoryResponse getHistory(Integer userId, Integer familyId, Integer childId, LocalDate startDate,
		LocalDate endDate);
}
