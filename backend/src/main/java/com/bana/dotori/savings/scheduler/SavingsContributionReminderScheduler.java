package com.bana.dotori.savings.scheduler;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bana.dotori.savings.service.SavingsContributionReminderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 적금 납입 리마인더 크론.
 *
 * <p>저녁 7시에 돈다. 다른 크론들은 자정 근처에 몰려 있는데(집안일 00:00, 투자 00:05,
 * 예적금 정산 00:20) 그것들은 사람이 볼 필요가 없는 정리 작업이다. 이 알림은 아이가
 * 받아서 바로 움직여야 하는 것이라 앱을 열 만한 시간에 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavingsContributionReminderScheduler {

	private static final String KST_ZONE = "Asia/Seoul";
	private static final ZoneId KST = ZoneId.of(KST_ZONE);

	private final SavingsContributionReminderService reminderService;

	@Scheduled(cron = "0 0 19 * * *", zone = KST_ZONE)
	public void remindContributions() {
		int sent = reminderService.remindUnpaid(OffsetDateTime.now(KST));
		if (sent > 0) {
			log.info("적금 납입 리마인더 발송. 대상={}건", sent);
		}
	}
}
