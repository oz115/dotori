package com.bana.dotori.quiz.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 오늘의 퀴즈 조회 (QUIZ-02). {@code choices}는 AI가 객관식이 아니라 주관식으로만 생성하므로
 * 항상 null이다 — 명세 필드라 남겨두되 채우지 않는다.
 *
 * <p>{@code correctAnswer}·{@code explanation}은 명세에 없는 필드다. 제출을 3회 다 썼거나
 * 정답을 맞혀 더 이상 제출할 수 없는 상태(alreadySubmitted=true)일 때만 채워서, 화면을
 * 나갔다가 다시 들어와도 정답을 볼 수 있게 한다 (팀 결정).
 *
 * <p>{@code attemptCount}도 명세에 없는 필드다. 화면을 나갔다가 다시 들어와도 "도전 기회
 * n/3" 표시가 정확하도록, 지금까지 제출한 시도 수를 그대로 내려준다 (팀 결정).
 *
 * <p>{@code hint}도 명세에 없는 필드다. 한 번이라도 틀렸고 아직 재도전할 수 있을 때만 정답의
 * 초성을 채운다. 정답이 공개되는 상태(alreadySubmitted=true)이거나 아직 한 번도 안 풀었으면
 * null이다. 정답 자체는 잠기기 전까지 내려주지 않으므로 초성은 서버가 만들어 준다.
 */
public record QuizTodayResponse(
	Integer quizId,
	LocalDate quizDate,
	String question,
	List<String> choices,
	int rewardPoints,
	boolean alreadySubmitted,
	String correctAnswer,
	String explanation,
	int attemptCount,
	String hint
) {
}
