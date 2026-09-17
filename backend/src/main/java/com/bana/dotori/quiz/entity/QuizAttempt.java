package com.bana.dotori.quiz.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 퀴즈 제출 1회 (QUIZ-03). 재도전을 허용하므로 같은 quizId+childId로 여러 행이 쌓일 수 있다
 * (유니크 제약 없음, 최대 3행 — {@code QuizServiceImpl}이 강제한다).
 */
@Entity
@Table(name = "quiz_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "quiz_attempt_id")
	private Integer id;

	@Column(name = "quiz_id", nullable = false)
	private Integer quizId;

	@Column(name = "child_id", nullable = false)
	private Integer childId;

	@Column(name = "submitted_answer", length = 255, nullable = false)
	private String submittedAnswer;

	@Column(name = "is_correct", nullable = false)
	private boolean correct;

	@Column(name = "earned_points", nullable = false)
	private int earnedPoints;

	@CreationTimestamp
	@Column(name = "submitted_at", nullable = false, updatable = false)
	private OffsetDateTime submittedAt;

	@Builder
	private QuizAttempt(Integer quizId, Integer childId, String submittedAnswer, boolean correct, int earnedPoints) {
		this.quizId = quizId;
		this.childId = childId;
		this.submittedAnswer = submittedAnswer;
		this.correct = correct;
		this.earnedPoints = earnedPoints;
	}
}
