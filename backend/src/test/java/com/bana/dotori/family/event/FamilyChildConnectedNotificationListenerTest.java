package com.bana.dotori.family.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class FamilyChildConnectedNotificationListenerTest {

	private static final Integer FAMILY_ID = 96;
	private static final Integer CHILD_ID = 247;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private FamilyChildConnectedNotificationListener listener;

	private FamilyMember parentMember(Integer userId) {
		User parent = User.builder()
			.loginId("parent" + userId)
			.password("pw")
			.name("부모")
			.email("parent" + userId + "@example.com")
			.role(UserRole.PARENT)
			.build();
		ReflectionTestUtils.setField(parent, "id", userId);
		return FamilyMember.builder().user(parent).build();
	}

	private FamilyChildConnectedEvent event() {
		return new FamilyChildConnectedEvent(FAMILY_ID, CHILD_ID, "민준", "도토리네");
	}

	@Test
	@DisplayName("가족의 부모 전원에게 누가 들어왔는지 알린다")
	void 부모_전원에게() {
		// 코드를 건네준 부모와 앱을 보는 부모가 다를 수 있어서 만든 사람만 알면 부족하다.
		given(familyMemberRepository.findAllByFamilyIdAndRole(FAMILY_ID, UserRole.PARENT))
			.willReturn(List.of(parentMember(1), parentMember(2)));

		listener.onChildConnected(event());

		then(pushNotificationService).should()
			.notify(eq(1), eq(NotificationType.INVITATION), any(), contains("민준"), eq(FAMILY_ID));
		then(pushNotificationService).should()
			.notify(eq(2), eq(NotificationType.INVITATION), any(), contains("민준"), eq(FAMILY_ID));
	}

	@Test
	@DisplayName("합류한 아이에게는 어느 가족인지 알린다")
	void 아이에게는_가족_이름() {
		given(familyMemberRepository.findAllByFamilyIdAndRole(FAMILY_ID, UserRole.PARENT))
			.willReturn(List.of());

		listener.onChildConnected(event());

		then(pushNotificationService).should()
			.notify(eq(CHILD_ID), eq(NotificationType.INVITATION), any(), contains("도토리네"), eq(FAMILY_ID));
	}

	@Test
	@DisplayName("부모가 없어도 아이 알림은 나간다")
	void 부모가_없어도() {
		// 가족을 만든 부모가 탈퇴한 뒤 코드가 살아 있는 경우처럼 드물지만 가능한 상태다.
		given(familyMemberRepository.findAllByFamilyIdAndRole(FAMILY_ID, UserRole.PARENT))
			.willReturn(List.of());

		listener.onChildConnected(event());

		then(pushNotificationService).should(times(1)).notify(any(), any(), any(), any(), any());
	}
}
