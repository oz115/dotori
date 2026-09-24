package com.bana.dotori.notification.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bana.dotori.notification.service.DailyBriefingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 하루 시작 알림 크론.
 *
 * <p>아침 9시에 돈다. 오늘 치 퀴즈와 뉴스는 자정 직후에 이미 만들어져 있고(00:05 뉴스,
 * 00:10 퀴즈), 아이가 앱을 열 만한 시간에 알리는 것이 목적이라 생성 시각과 떼어 뒀다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBriefingScheduler {

	private static final String KST = "Asia/Seoul";

	private final DailyBriefingService dailyBriefingService;

	@Scheduled(cron = "0 0 9 * * *", zone = KST)
	public void sendDailyBriefing() {
		int sent = dailyBriefingService.notifyChildren();
		log.info("하루 시작 알림 발송. 대상={}명", sent);
	}
}
