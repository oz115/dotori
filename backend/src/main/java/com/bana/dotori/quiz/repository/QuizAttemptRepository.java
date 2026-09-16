package com.bana.dotori.quiz.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.quiz.dto.QuizAttemptHistoryItem;
import com.bana.dotori.quiz.entity.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {

	/**
	 * 이 퀴즈에 이 자녀가 지금까지 제출한 시도 전부 (최대 3건). 정답 처리 여부와 시도 횟수를
	 * 판단하는 데 쓴다.
	 */
	List<QuizAttempt> findByQuizIdAndChildId(Integer quizId, Integer childId);

	/**
	 * 퀴즈 이력 조회 (QUIZ-01). 문제·날짜는 {@code quiz_attempts}가 아니라 {@code quizzes}에
	 * 있어서 조인한다 — {@code ChoreCompletionRequestRepository#findSummaries}와 같은 패턴이다.
	 * startDate·endDate가 null이면 그 조건은 건너뛴다(기간 제한 없음).
	 */
	@Query("select new com.bana.dotori.quiz.dto.QuizAttemptHistoryItem("
		+ "a.id, q.quizDate, q.question, a.submittedAnswer, a.correct, a.earnedPoints, a.submittedAt) "
		+ "from QuizAttempt a, Quiz q "
		+ "where a.quizId = q.id and a.childId = :childId "
		+ "and (:startDate is null or q.quizDate >= :startDate) "
		+ "and (:endDate is null or q.quizDate <= :endDate) "
		+ "order by q.quizDate desc, a.submittedAt desc")
	List<QuizAttemptHistoryItem> findHistory(@Param("childId") Integer childId,
		@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
