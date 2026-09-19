package com.bana.dotori.internal.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.scheduler.ChoreRepeatScheduler;
import com.bana.dotori.internal.config.QaTriggerProperties;
import com.bana.dotori.investment.client.AiInvestmentClient;
import com.bana.dotori.notification.scheduler.DailyBriefingScheduler;
import com.bana.dotori.quiz.client.AiQuizClient;
import com.bana.dotori.savings.scheduler.SavingsContributionReminderScheduler;
import com.bana.dotori.savings.scheduler.SavingsSettlementScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 자정 전후로 도는 배치 6개(할 일 반복 · 예적금 정산 · 적금 리마인더 · 하루 시작 알림 ·
 * 투자 뉴스·시세 · 퀴즈 생성)를 하루를 기다리지 않고 QA 중에 수동으로 재현하기 위한 임시 트리거.
 *
 * <p>각 스케줄러가 크론에서 부르는 메서드를 그대로 다시 부르는 것뿐이라 배치 자체의 판단
 * 로직·부수효과는 바꾸지 않는다. 대상 자녀를 지정하는 파라미터는 없다 — 조건에 맞는
 * 전체 가족·자녀가 그대로 대상이 된다(실제 포인트 지급, 실제 푸시 발송, 실제 LLM 호출 포함).
 * 공유 QA DB가 아니라 언제 날려도 되는 로컬 개발 DB에서만 쓰는 걸 전제로 한다.
 *
 * <p>날짜·시각 컬럼을 하루 백데이트하는 건 이 컨트롤러의 몫이 아니다 — 그건 QA 가이드의 SQL로
 * 미리 해두고, 이 컨트롤러는 그다음 "배치를 지금 돌려라"만 대신한다.
 *
 * <p>{@link QaTriggerProperties#enabled()} 가 꺼져 있으면 이 빈 자체가 생기지 않는다.
 * 요청마다 {@code X-Internal-Token} 헤더를 AI 서버 호출에 쓰는 것과 같은
 * {@code INTERNAL_TOKEN} 값과 대조한다 — Security 설정에서 이 경로는 permitAll 이라
 * JWT 대신 이 토큰이 유일한 잠금이다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/qa")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "qa-trigger.enabled", havingValue = "true")
public class QaTriggerController {

	private static final String TOKEN_HEADER = "X-Internal-Token";

	private final ChoreRepeatScheduler choreRepeatScheduler;
	private final SavingsSettlementScheduler savingsSettlementScheduler;
	private final SavingsContributionReminderScheduler savingsContributionReminderScheduler;
	private final DailyBriefingScheduler dailyBriefingScheduler;
	// investmentDailyScheduler/quizDailyScheduler(스케줄러)가 아니라 그 안의 AI 클라이언트를
	// 직접 부른다 — 두 스케줄러는 실제 무인 크론용으로 RestClientException 을 로그만 남기고
	// 삼키게 만들어져 있어서(InvestmentDailyScheduler.java, QuizDailyScheduler.java), 그걸 그대로
	// 부르면 AI 호출이 401/타임아웃으로 실패해도 이 컨트롤러는 "ok"로만 보게 된다.
	private final AiInvestmentClient aiInvestmentClient;
	private final AiQuizClient aiQuizClient;
	private final QaTriggerProperties qaTriggerProperties;

	@PostMapping("/chores/roll")
	public ResponseEntity<Void> rollChores(
			@RequestParam ChoreRepeatType repeatType,
			@RequestHeader(TOKEN_HEADER) String token) {
		if (!isAuthorized(token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		switch (repeatType) {
			case DAY -> choreRepeatScheduler.rollDaily();
			case WEEK -> choreRepeatScheduler.rollWeekly();
			case MONTH -> choreRepeatScheduler.rollMonthly();
			default -> {
				return ResponseEntity.badRequest().build();
			}
		}
		return ResponseEntity.ok().build();
	}

	@PostMapping("/savings/settle")
	public ResponseEntity<Void> settleSavings(@RequestHeader(TOKEN_HEADER) String token) {
		if (!isAuthorized(token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		savingsSettlementScheduler.runWeeklySettlement();
		return ResponseEntity.ok().build();
	}

	@PostMapping("/savings/remind")
	public ResponseEntity<Void> remindSavings(@RequestHeader(TOKEN_HEADER) String token) {
		if (!isAuthorized(token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		savingsContributionReminderScheduler.remindContributions();
		return ResponseEntity.ok().build();
	}

	@PostMapping("/notifications/daily-briefing")
	public ResponseEntity<Void> sendDailyBriefing(@RequestHeader(TOKEN_HEADER) String token) {
		if (!isAuthorized(token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		dailyBriefingScheduler.sendDailyBriefing();
		return ResponseEntity.ok().build();
	}

	/**
	 * 6개 배치를 실제 크론 순서(자정 전후 → 저녁 리마인더 → 아침 브리핑)대로 한 번에 돌린다.
	 * 데이터 백데이트는 여전히 QA 가이드의 SQL로 먼저 해 둬야 한다 — 여기서는 안 한다.
	 *
	 * <p>한 단계가 실패해도 나머지는 계속 돈다(정산 스케줄러의 건별 처리와 같은 방침).
	 * 어떤 단계가 실패했는지는 응답의 각 키 값으로 확인한다.
	 */
	@PostMapping("/run-all")
	public ResponseEntity<Map<String, String>> runAll(@RequestHeader(TOKEN_HEADER) String token) {
		if (!isAuthorized(token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Map<String, String> result = new LinkedHashMap<>();
		runStep(result, "choresRollDay", choreRepeatScheduler::rollDaily);
		runStep(result, "choresRollWeek", choreRepeatScheduler::rollWeekly);
		runStep(result, "choresRollMonth", choreRepeatScheduler::rollMonthly);
		runStep(result, "investmentDaily", aiInvestmentClient::runDailyUpdate);
		runStep(result, "quizGenerate", aiQuizClient::generateToday);
		runStep(result, "savingsRemind", savingsContributionReminderScheduler::remindContributions);
		runStep(result, "savingsSettle", savingsSettlementScheduler::runWeeklySettlement);
		runStep(result, "dailyBriefing", dailyBriefingScheduler::sendDailyBriefing);
		return ResponseEntity.ok(result);
	}

	private void runStep(Map<String, String> result, String key, Runnable step) {
		try {
			step.run();
			result.put(key, "ok");
		} catch (RuntimeException e) {
			log.error("QA run-all 단계 실패: {}", key, e);
			result.put(key, "failed: " + e.getMessage());
		}
	}

	// AI 서버 쪽과 같은 관례: 로컬에서 INTERNAL_TOKEN 을 비워 두면 검증을 생략한다.
	// 어차피 QA_TRIGGER_ENABLED 로 이미 한 번 막혀 있어야만 이 빈이 존재한다.
	private boolean isAuthorized(String token) {
		String expected = qaTriggerProperties.token();
		return expected.isBlank() || expected.equals(token);
	}
}
