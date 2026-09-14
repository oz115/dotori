package com.bana.dotori.investment.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.bana.dotori.investment.client.AiDailyInvestmentResponse;
import com.bana.dotori.investment.client.AiInvestmentClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NEWS-02·03 — 오늘의 투자 뉴스 생성과 가족별 가격 갱신을 AI 서버에 위임하는 크론.
 *
 * <p>실제 로직(LLM 으로 뉴스 3건 생성, 어제 뉴스로 오늘 가격 갱신)은 AI 서버가 갖고
 * 있고 여기는 하루 한 번 트리거만 한다. AI 쪽 두 작업 모두 멱등이라(이미 처리된 날은
 * 그대로 반환) 재호출·중복 호출이 안전하다.
 *
 * <p>AI 의 회사 로테이션은 "이번 주 등장 횟수"를 뉴스 이력에서 역산하므로 하루 한 번은
 * 반드시 호출돼야 로테이션·주간 수익 한도가 어긋나지 않는다. 실패하면 로그만 남긴다 —
 * 급하면 AI 서버의 POST /internal/investment/daily 를 손으로 호출해 복구한다(멱등).
 *
 * <p>자정 정각은 집안일 크론(ChoreRepeatScheduler)과 겹쳐 5분 비껴 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvestmentDailyScheduler {

	private static final String KST = "Asia/Seoul";

	private final AiInvestmentClient aiInvestmentClient;

	@Scheduled(cron = "0 5 0 * * *", zone = KST)
	public void runDailyUpdate() {
		try {
			AiDailyInvestmentResponse response = aiInvestmentClient.runDailyUpdate();
			log.info("투자 일일 갱신 완료 — 가격 이력 {}건 기록", response.priceUpdatesWritten());
		} catch (RestClientException e) {
			log.error("투자 일일 갱신 실패 — AI 서버 호출 오류. 멱등이라 재호출로 복구할 수 있다", e);
		}
	}
}
