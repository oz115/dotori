package com.bana.dotori.investment.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;

import com.bana.dotori.family.event.FamilyOnboardedEvent;
import com.bana.dotori.investment.client.AiInvestmentClient;
import com.bana.dotori.investment.client.AiPriceBackfillResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 가족 온보딩 직후 AI 서버에 가격 이력 백필을 트리거한다 — 가입 당일에도 투자 차트에
 * 일주일치 흐름이 보이게 한다.
 *
 * <p>AFTER_COMMIT 이어야 한다. AI 서버가 DB 에서 가족 행(용돈·한도)을 직접 읽으므로,
 * 커밋 전에 부르면 아직 없는 가족을 조회해 아무것도 안 쓴다.
 *
 * <p>실패해도 온보딩은 이미 성공한 뒤고, 다음 00:05 일일 갱신이 같은 백필을 해 주므로
 * 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FamilyPriceBackfillListener {

	private final AiInvestmentClient aiInvestmentClient;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onFamilyOnboarded(FamilyOnboardedEvent event) {
		try {
			AiPriceBackfillResponse response = aiInvestmentClient.backfillFamilyPrices(event.familyId());
			log.info("가족 온보딩 가격 백필 완료 — familyId={}, 이력 {}건", event.familyId(),
				response.priceUpdatesWritten());
		} catch (RestClientException e) {
			log.error("가족 온보딩 가격 백필 실패 — familyId={}. 다음 일일 갱신이 대신 백필한다",
				event.familyId(), e);
		}
	}
}
