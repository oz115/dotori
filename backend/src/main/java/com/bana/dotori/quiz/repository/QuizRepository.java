package com.bana.dotori.quiz.repository;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.quiz.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Integer> {

	Optional<Quiz> findByFamilyIdAndChildIdAndQuizDate(Integer familyId, Integer childId, LocalDate quizDate);

	/**
	 * 제출 전에 퀴즈 행을 {@code SELECT ... FOR UPDATE}로 잠근다.
	 *
	 * <p>같은 퀴즈에 대한 동시 제출이 이 지점에서 한 줄로 세워진다. 잠그지 않으면 동시에 들어온
	 * 두 요청이 각자 "아직 3회 미만/정답 없음"을 읽고 둘 다 통과해 최대 시도 횟수를 넘기거나
	 * 정답을 두 번 지급할 수 있다 ({@code ChoreCompletionRequestRepository#findByIdForUpdate}와
	 * 같은 이유).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select q from Quiz q where q.id = :id")
	Optional<Quiz> findByIdForUpdate(@Param("id") Integer id);
}
