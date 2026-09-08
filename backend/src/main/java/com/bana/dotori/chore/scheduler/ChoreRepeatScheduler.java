package com.bana.dotori.chore.scheduler;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.service.ChoreRepeatRollService;

import lombok.RequiredArgsConstructor;

/**
 * 반복 할 일의 기간을 주기마다 다음 주기로 미는 크론.
 *
 * <p>세 개로 나눈 이유는 주기마다 밀어야 하는 시점이 다르기 때문이다.
 * 매일 도는 하나로 합치면 주간·월간 할 일이 주기 중간에 갱신된다.
 *
 * <table>
 *   <tr><th>주기</th><th>실행 시점</th><th>새 기간</th></tr>
 *   <tr><td>DAY</td><td>매일 자정</td><td>오늘 하루</td></tr>
 *   <tr><td>WEEK</td><td>매주 월요일 자정</td><td>이번 주 월~일</td></tr>
 *   <tr><td>MONTH</td><td>매월 1일 자정</td><td>이번 달 1일~말일</td></tr>
 * </table>
 *
 * <p>타임존을 KST 로 고정한다. 서버 타임존에 맡기면 배포 환경이 UTC 일 때
 * 한국 시간 오전 9시에 "자정" 작업이 돈다.
 *
 * <p>주의: 이 크론은 인스턴스마다 돈다. 서버를 여러 대로 늘리면 같은 갱신이
 * 중복 실행된다. 갱신 자체는 멱등이라(같은 기간을 다시 써도 결과가 같다) 지금은
 * 문제가 없지만, 스케일 아웃 시점에는 분산 락이나 스케줄러 분리가 필요하다.
 */
@Component
@RequiredArgsConstructor
public class ChoreRepeatScheduler {

	private static final String KST = "Asia/Seoul";

	private final ChoreRepeatRollService choreRepeatRollService;
	private final Clock clock;

	@Scheduled(cron = "0 0 0 * * *", zone = KST)
	public void rollDaily() {
		choreRepeatRollService.roll(ChoreRepeatType.DAY, today());
	}

	@Scheduled(cron = "0 0 0 * * MON", zone = KST)
	public void rollWeekly() {
		choreRepeatRollService.roll(ChoreRepeatType.WEEK, today());
	}

	@Scheduled(cron = "0 0 0 1 * *", zone = KST)
	public void rollMonthly() {
		choreRepeatRollService.roll(ChoreRepeatType.MONTH, today());
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}
}
