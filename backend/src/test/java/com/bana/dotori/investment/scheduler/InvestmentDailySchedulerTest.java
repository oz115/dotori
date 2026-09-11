package com.bana.dotori.investment.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import com.bana.dotori.investment.client.AiDailyInvestmentResponse;
import com.bana.dotori.investment.client.AiInvestmentClient;

@ExtendWith(MockitoExtension.class)
class InvestmentDailySchedulerTest {

	@Mock
	private AiInvestmentClient aiInvestmentClient;

	@InjectMocks
	private InvestmentDailyScheduler investmentDailyScheduler;

	@Test
	@DisplayName("일일 갱신은 AI 서버 호출에 위임한다")
	void runDailyUpdate_delegatesToAiServer() {
		given(aiInvestmentClient.runDailyUpdate()).willReturn(new AiDailyInvestmentResponse(30));

		investmentDailyScheduler.runDailyUpdate();

		then(aiInvestmentClient).should().runDailyUpdate();
	}

	@Test
	@DisplayName("AI 서버 호출이 실패해도 크론이 예외로 죽지 않는다 (멱등이라 다음 호출로 복구)")
	void runDailyUpdate_swallowsClientFailure() {
		given(aiInvestmentClient.runDailyUpdate())
			.willThrow(new ResourceAccessException("connection refused"));

		assertThatCode(() -> investmentDailyScheduler.runDailyUpdate()).doesNotThrowAnyException();
	}
}
