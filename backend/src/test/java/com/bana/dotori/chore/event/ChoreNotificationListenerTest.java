package com.bana.dotori.chore.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

/**
 * 집안일 알림이 "누구에게" 가는지와 "무엇을 담는지" 를 고정한다 (S15P21A505-246).
 *
 * <p>받는 사람이 뒤바뀌는 것이 이 기능에서 가장 아픈 오류다 — 아이의 완료 요청이 아이에게
 * 가거나 승인이 부모에게 가면 알림이 아예 없느니만 못하다.
 */
@ExtendWith(MockitoExtension.class)
class ChoreNotificationListenerTest {

	private static final Integer CHORE_ID = 7;
	private static final Integer CHILD_ID = 2;
	private static final Integer PARENT_ID = 1;
	private static final String TITLE = "설거지 하기";

	@Nested
	@ExtendWith(MockitoExtension.class)
	@DisplayName("할 일 등록")
	class Created {

		@Mock
		private PushNotificationService pushNotificationService;

		@InjectMocks
		private ChoreCreatedNotificationListener listener;

		@Test
		@DisplayName("담당 아이에게 CHORE 알림을 보낸다")
		void 아이에게_간다() {
			listener.onChoreCreated(created(TITLE));

			then(pushNotificationService).should()
				.notify(eq(CHILD_ID), eq(NotificationType.CHORE), eq("새로운 미션이 생겼어요"),
					eq(TITLE + "라는 제목의 미션을 하러 가볼까요?"), eq(CHORE_ID));
		}

		@Test
		@DisplayName("긴 제목은 줄여서 담는다 — 뒤에 붙는 안내 문구가 밀려나지 않게")
		void 긴_제목은_줄인다() {
			// 제목은 100자까지 허용된다. 그대로 넣으면 알림함 두 줄이 제목만으로 차고
			// "하러 가볼까요?" 가 화면 밖으로 밀려난다 (QA).
			listener.onChoreCreated(created("가".repeat(40)));

			then(pushNotificationService).should()
				.notify(eq(CHILD_ID), eq(NotificationType.CHORE), any(),
					eq("가".repeat(15) + "...라는 제목의 미션을 하러 가볼까요?"), eq(CHORE_ID));
		}

		private ChoreCreatedEvent created(String title) {
			return new ChoreCreatedEvent(CHORE_ID, 10, CHILD_ID, title, null,
				ChoreDifficulty.NORMAL, ChoreRepeatType.ONCE);
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	@DisplayName("완료 요청 생애주기")
	class Completion {

		@Mock
		private UserRepository userRepository;

		@Mock
		private PushNotificationService pushNotificationService;

		@InjectMocks
		private ChoreCompletionNotificationListener listener;

		private void givenChildName(String name) {
			User child = User.builder()
				.loginId("child")
				.password("pw")
				.name(name)
				.email("child@example.com")
				.role(UserRole.CHILD)
				.build();
			given(userRepository.findById(CHILD_ID)).willReturn(Optional.of(child));
		}

		@Test
		@DisplayName("완료 요청은 할 일을 등록한 부모에게 가고 아이 이름이 담긴다")
		void 요청은_부모에게() {
			givenChildName("민준");

			listener.onRequested(new ChoreCompletionRequestedEvent(CHORE_ID, PARENT_ID, TITLE, CHILD_ID));

			then(pushNotificationService).should()
				.notify(eq(PARENT_ID), eq(NotificationType.CHORE), any(), contains("민준"), eq(CHORE_ID));
		}

		@Test
		@DisplayName("아이를 못 찾아도 알림은 나간다")
		void 이름을_못_찾아도_보낸다() {
			// 이름은 문구의 일부일 뿐이다. 조회 실패로 알림이 통째로 사라지면 부모는
			// 완료 요청이 온 것을 영영 모른다.
			given(userRepository.findById(CHILD_ID)).willReturn(Optional.empty());

			listener.onRequested(new ChoreCompletionRequestedEvent(CHORE_ID, PARENT_ID, TITLE, CHILD_ID));

			then(pushNotificationService).should()
				.notify(eq(PARENT_ID), eq(NotificationType.CHORE), any(), contains(TITLE), eq(CHORE_ID));
		}

		@Test
		@DisplayName("승인은 요청한 아이에게 가고 실제 지급액이 담긴다")
		void 승인은_아이에게() {
			listener.onApproved(new ChoreCompletionApprovedEvent(CHORE_ID, CHILD_ID, TITLE, 500));

			then(pushNotificationService).should()
				.notify(eq(CHILD_ID), eq(NotificationType.CHORE), eq("완료 승인이 되었어요."),
					eq(TITLE + "라는 미션을 완료했어요. 500 도토리를 얻었어요."), eq(CHORE_ID));
		}

		@Test
		@DisplayName("거절은 요청한 아이에게 간다")
		void 거절은_아이에게() {
			listener.onRejected(new ChoreCompletionRejectedEvent(CHORE_ID, CHILD_ID, TITLE));

			then(pushNotificationService).should()
				.notify(eq(CHILD_ID), eq(NotificationType.CHORE), any(), contains(TITLE), eq(CHORE_ID));
		}
	}
}
