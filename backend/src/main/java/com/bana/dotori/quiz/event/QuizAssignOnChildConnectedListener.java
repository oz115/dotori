package com.bana.dotori.quiz.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;

import com.bana.dotori.family.event.FamilyChildConnectedEvent;
import com.bana.dotori.quiz.client.AiQuizAssignResponse;
import com.bana.dotori.quiz.client.AiQuizClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 아이가 가족에 합류하면 그 가족의 오늘자 퀴즈를 바로 배정한다.
 *
 * <p>일일 크론({@code QuizDailyScheduler})은 00:10 에 한 번 돌면서 그 시점에 DB 에 있던 자녀만
 * 훑는다. 그래서 낮에 합류한 아이는 다음 날 자정까지 오늘의 퀴즈 화면이 QUIZ_NOT_READY 였다.
 * 크론은 그대로 두고, 늦게 들어온 가족만 여기서 따로 메꾼다.
 *
 * <p>AFTER_COMMIT 이라 합류가 롤백되면 호출되지 않는다. 실패해도 합류 자체는 이미 끝났으므로
 * 로그만 남긴다 — 다음 날 00:10 크론이 같은 아이를 다시 훑어서 결국 채워진다.
 *
 * <p>둘째 아이가 합류한 경우처럼 형제가 이미 오늘 문제를 받았으면 AI 가 그 문제를 복사하므로
 * LLM 호출은 나가지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizAssignOnChildConnectedListener {

	private final AiQuizClient aiQuizClient;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onChildConnected(FamilyChildConnectedEvent event) {
		try {
			AiQuizAssignResponse response = aiQuizClient.assignFamily(event.familyId());
			log.info("가족 {} 합류 직후 퀴즈 배정 — 티어 {}, 삽입 {}건, 건너뜀 {}건, 형제 문제 재사용 {}",
				event.familyId(), response.tier(), response.inserted(), response.skipped(),
				response.reusedSiblingQuiz());
		} catch (RestClientException e) {
			log.error("가족 {} 합류 직후 퀴즈 배정 실패 — 다음 일일 크론이 메꾼다", event.familyId(), e);
		}
	}
}
