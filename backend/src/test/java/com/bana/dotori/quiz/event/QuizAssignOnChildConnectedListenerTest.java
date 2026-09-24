package com.bana.dotori.quiz.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import com.bana.dotori.family.event.FamilyChildConnectedEvent;
import com.bana.dotori.quiz.client.AiQuizAssignResponse;
import com.bana.dotori.quiz.client.AiQuizClient;

@ExtendWith(MockitoExtension.class)
class QuizAssignOnChildConnectedListenerTest {

	private static final Integer FAMILY_ID = 96;
	private static final Integer CHILD_ID = 247;

	@Mock
	private AiQuizClient aiQuizClient;

	@InjectMocks
	private QuizAssignOnChildConnectedListener listener;

	private FamilyChildConnectedEvent event() {
		return new FamilyChildConnectedEvent(FAMILY_ID, CHILD_ID, "민준", "도토리네");
	}

	@Test
	@DisplayName("아이가 합류하면 그 가족의 오늘자 퀴즈를 바로 배정한다")
	void 합류_직후_배정() {
		// 자정 크론은 그 시점의 자녀만 훑어서, 낮에 들어온 아이는 다음 날까지 퀴즈가 없었다.
		given(aiQuizClient.assignFamily(FAMILY_ID))
			.willReturn(new AiQuizAssignResponse(FAMILY_ID, "BEGINNER", 3, 1, 0, 0, false));

		listener.onChildConnected(event());

		then(aiQuizClient).should().assignFamily(FAMILY_ID);
	}

	@Test
	@DisplayName("AI 호출이 실패해도 합류를 되돌리지 않는다")
	void 실패해도_삼킨다() {
		// AFTER_COMMIT 이라 합류는 이미 끝났다. 여기서 예외를 올려도 되돌릴 게 없고,
		// 다음 날 00:10 크론이 같은 아이를 다시 훑어서 결국 채워진다.
		willThrow(new RestClientException("AI 서버 응답 없음")).given(aiQuizClient).assignFamily(FAMILY_ID);

		listener.onChildConnected(event());

		then(aiQuizClient).should().assignFamily(FAMILY_ID);
	}
}
