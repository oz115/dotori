package com.bana.dotori.quiz.entity;

import java.time.LocalDate;

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
 * AI 서버가 매일 배정하는 퀴즈 1건 (QUIZ). {@code quiz_id} 는 (가족, 자녀, 날짜)마다
 * 하나씩 새로 생기므로 이미 자녀별로 유일하다 — 같은 문제라도 자녀마다 다른 행이다.
 *
 * <p>이 엔티티는 읽기 전용이다. 생성은 AI 서버가 {@code POST /internal/quiz/generate}로
 * 직접 INSERT 하고, 백엔드는 {@link com.bana.dotori.quiz.client.AiQuizClient}로 트리거만 한다.
 *
 * <p>{@code reward_points} 컬럼은 일부러 매핑하지 않는다. AI가 채워 넣는 값은 정식 산정식이
 * 나오기 전의 임시 티어값(10/20/30)이고, 실제 지급액은 백엔드가
 * {@code familySettings.weeklyRewardLimit() / 35} 공식으로 매번 새로 계산한다 (팀 결정,
 * AI 쪽 코드는 건드리지 않기로 함).
 */
@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz {

	private static final String REWARD_KEY_PREFIX = "quiz:";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "quiz_id")
	private Integer id;

	@Column(name = "family_id", nullable = false)
	private Integer familyId;

	@Column(name = "child_id", nullable = false)
	private Integer childId;

	@Column(name = "word_id", nullable = false)
	private Integer wordId;

	@Column(name = "question", nullable = false)
	private String question;

	@Column(name = "correct_answer", length = 255, nullable = false)
	private String correctAnswer;

	@Column(name = "explanation", length = 255)
	private String explanation;

	@Column(name = "quiz_date", nullable = false)
	private LocalDate quizDate;

	@Builder
	private Quiz(Integer familyId, Integer childId, Integer wordId, String question, String correctAnswer,
		String explanation, LocalDate quizDate) {
		this.familyId = familyId;
		this.childId = childId;
		this.wordId = wordId;
		this.question = question;
		this.correctAnswer = correctAnswer;
		this.explanation = explanation;
		this.quizDate = quizDate;
	}

	/**
	 * 이 퀴즈의 정답 보상 지급 멱등키. {@code quiz_id}가 이미 자녀별로 유일해서 chore처럼
	 * 주기를 따로 넣을 필요가 없다 — 최초 정답 1회에만 지급되고, 이후 제출은 서비스가
	 * 이미 정답 처리된 시도를 보고 막아서 여기까지 오지 않는다.
	 */
	public String rewardIdempotencyKey() {
		return REWARD_KEY_PREFIX + id;
	}
}
