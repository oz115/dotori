package com.bana.dotori.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class DailyBriefingServiceTest {

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private DailyBriefingService dailyBriefingService;

	private FamilyMember childMember(Integer userId) {
		User child = User.builder()
			.loginId("child" + userId)
			.password("pw")
			.name("아이")
			.email("child" + userId + "@example.com")
			.role(UserRole.CHILD)
			.build();
		ReflectionTestUtils.setField(child, "id", userId);
		return FamilyMember.builder().user(child).build();
	}

	@Test
	@DisplayName("가족에 속한 아이 전원에게 DAILY 알림을 보낸다")
	void 아이_전원에게() {
		given(familyMemberRepository.findAllActiveByRole(UserRole.CHILD))
			.willReturn(List.of(childMember(11), childMember(12)));

		int sent = dailyBriefingService.notifyChildren();

		assertThat(sent).isEqualTo(2);
		then(pushNotificationService).should()
			.notify(eq(11), eq(NotificationType.DAILY), any(), any(), isNull());
		then(pushNotificationService).should()
			.notify(eq(12), eq(NotificationType.DAILY), any(), any(), isNull());
	}

	@Test
	@DisplayName("부모에게는 보내지 않는다")
	void 부모는_제외() {
		// 퀴즈·뉴스는 아이 콘텐츠다. 역할을 인자로 넘기므로 조회 단계에서 갈린다.
		dailyBriefingService.notifyChildren();

		then(familyMemberRepository).should().findAllActiveByRole(UserRole.CHILD);
		then(familyMemberRepository).should(never()).findAllActiveByRole(UserRole.PARENT);
	}

	@Test
	@DisplayName("대상이 없으면 아무것도 보내지 않는다")
	void 대상이_없으면() {
		given(familyMemberRepository.findAllActiveByRole(UserRole.CHILD)).willReturn(List.of());

		int sent = dailyBriefingService.notifyChildren();

		assertThat(sent).isZero();
		then(pushNotificationService).should(never()).notify(any(), any(), any(), any(), any());
	}
}
