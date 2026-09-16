package com.bana.dotori.quiz.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.bana.dotori.quiz.client.AiQuizClient;
import com.bana.dotori.quiz.client.AiQuizGenerateResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QUIZ-AI-01·02 — 오늘자 퀴즈 생성·가족 배정을 AI 서버에 위임하는 크론.
 *
 * <p>자정 정각은 집안일 크론(00:00), 5분은 투자 크론(00:05)이 쓰므로 10분으로 비껴 둔다.
 * AI 쪽은 (child_id, quiz_date) 존재 여부로 멱등이라 재호출이 안전하다. 실패하면 로그만
 * 남긴다 — 급하면 AI 서버의 POST /internal/quiz/generate 를 손으로 호출해 복구한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizDailyScheduler {

	private static final String KST = "Asia/Seoul";

	private final AiQuizClient aiQuizClient;

	@Scheduled(cron = "0 10 0 * * *", zone = KST)
	public void generateToday() {
		try {
			AiQuizGenerateResponse response = aiQuizClient.generateToday();
			log.info("퀴즈 일일 생성 완료 — 풀 {}개, 삽입 {}건, 건너뜀 {}건, 실패 가족 {}건",
				response.poolSize(), response.inserted(), response.skipped(), response.failedFamilies());
		} catch (RestClientException e) {
			log.error("퀴즈 일일 생성 실패 — AI 서버 호출 오류. 멱등이라 재호출로 복구할 수 있다", e);
		}
	}
}
