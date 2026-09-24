package com.bana.dotori.quiz.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 부모용 오늘의 퀴즈 확인. 아이가 아직 안 풀었어도 문제와 <b>정답</b>을 내려준다 — 아이용
 * {@link QuizTodayResponse}는 잠기기 전에 정답을 null로 가리는데, 이 응답은 부모 전용
 * 엔드포인트에서만 나가므로 그 규칙과 무관하다.
 *
 * <p>{@code attempts}는 제출 순서(1회차부터)이고, 아직 안 풀었으면 빈 목록이다.
 *
 * @param attemptCount 지금까지 제출한 시도 수 (0~{@code maxAttempts})
 * @param maxAttempts  하루 최대 제출 횟수
 * @param solved       정답을 맞힌 시도가 있는지
 * @param locked       정답을 맞혔거나 횟수를 다 써서 더 제출할 수 없는지 (아이용 alreadySubmitted 와 같은 값)
 */
public record QuizParentTodayResponse(
	Integer quizId,
	LocalDate quizDate,
	String question,
	String correctAnswer,
	String explanation,
	int attemptCount,
	int maxAttempts,
	boolean solved,
	boolean locked,
	List<QuizParentAttemptItem> attempts
) {
}
