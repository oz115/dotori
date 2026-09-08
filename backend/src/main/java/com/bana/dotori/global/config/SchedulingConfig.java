package com.bana.dotori.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 크론 작업 설정.
 *
 * <p>Clock 을 빈으로 두는 이유는 "오늘"을 테스트에서 고정할 수 있게 하려는 것이다.
 * {@code LocalDate.now()} 를 직접 부르면 자정 경계나 월말 동작을 검증할 방법이 없다.
 *
 * <p>존은 KST 다. 서버 타임존에 맡기면 배포 환경이 UTC 일 때 크론의 "자정"과
 * 코드가 보는 "오늘"이 9시간 엇갈린다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}
}
