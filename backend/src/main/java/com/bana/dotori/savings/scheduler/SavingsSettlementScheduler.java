package com.bana.dotori.savings.scheduler;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bana.dotori.savings.service.SavingsSettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SAV 주간 정산 크론.
 *
 * <p><b>매일 돈다.</b> 정산 주기는 가입일 기준(가입 시각 + n주)이라 요일이 제각각이고,
 * "오늘 정산일이 돌아온 건"은 매일 생긴다. 하루 한 번 훑으면서 시각이 지난 건만 처리한다.
 *
 * <p>한 건이 실패해도 전체를 멈추지 않는다. 각 건은 자기 트랜잭션에서 커밋되고, 실패한 건은
 * {@code next_settlement_at} 이 그대로 남아 다음 날 다시 대상이 된다. 서버가 며칠 멈춰 있었다면
 * 밀린 주는 {@code settle} 이 한 번에 따라잡는다.
 *
 * <p>자정 정각(집안일)·00:05(투자)와 겹치지 않게 00:20 으로 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavingsSettlementScheduler {

	private static final String KST_ZONE = "Asia/Seoul";
	private static final ZoneId KST = ZoneId.of(KST_ZONE);

	private final SavingsSettlementService savingsSettlementService;

	@Scheduled(cron = "0 20 0 * * *", zone = KST_ZONE)
	public void runWeeklySettlement() {
		OffsetDateTime now = OffsetDateTime.now(KST);
		List<Integer> dueIds = savingsSettlementService.findDueSubscriptionIds(now);
		if (dueIds.isEmpty()) {
			return;
		}

		int settled = 0;
		int failed = 0;
		long totalInterest = 0;
		for (Integer subscriptionId : dueIds) {
			try {
				totalInterest += savingsSettlementService.settle(subscriptionId, now);
				settled++;
			} catch (RuntimeException e) {
				failed++;
				log.error("예적금 정산 실패 — 다음 날 다시 시도한다. savingsSubscriptionId={}", subscriptionId, e);
			}
		}

		log.info("예적금 정산 완료. 대상={}건, 성공={}건, 실패={}건, 지급·적립 이자 합계={}",
			dueIds.size(), settled, failed, totalInterest);
	}
}
