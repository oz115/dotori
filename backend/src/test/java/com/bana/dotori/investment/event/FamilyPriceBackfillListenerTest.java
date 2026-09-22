package com.bana.dotori.investment.event;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import com.bana.dotori.family.event.FamilyOnboardedEvent;
import com.bana.dotori.investment.client.AiInvestmentClient;
import com.bana.dotori.investment.client.AiPriceBackfillResponse;

@ExtendWith(MockitoExtension.class)
class FamilyPriceBackfillListenerTest {

	@Mock
	private AiInvestmentClient aiInvestmentClient;

	@InjectMocks
	private FamilyPriceBackfillListener listener;

	@Test
	@DisplayName("가족 온보딩 이벤트를 받으면 그 가족의 가격 백필을 AI 서버에 요청한다")
	void 온보딩_이벤트가_백필을_트리거() {
		BDDMockito.given(aiInvestmentClient.backfillFamilyPrices(7))
			.willReturn(new AiPriceBackfillResponse(80));

		listener.onFamilyOnboarded(new FamilyOnboardedEvent(7));

		BDDMockito.then(aiInvestmentClient).should().backfillFamilyPrices(7);
	}

	@Test
	@DisplayName("AI 호출이 실패해도 예외를 삼킨다 — 온보딩은 이미 성공했고 다음 크론이 대신 백필한다")
	void 백필_실패는_전파하지_않는다() {
		BDDMockito.given(aiInvestmentClient.backfillFamilyPrices(7))
			.willThrow(new RestClientException("AI 서버 연결 실패"));

		assertThatCode(() -> listener.onFamilyOnboarded(new FamilyOnboardedEvent(7)))
			.doesNotThrowAnyException();
	}
}
