package com.bana.dotori.store.event;

import static org.mockito.ArgumentMatchers.any;
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
import com.bana.dotori.store.entity.StoreProductCategory;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class StoreProductRegisteredNotificationListenerTest {

	private static final Integer FAMILY_ID = 10;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private StoreProductRegisteredNotificationListener listener;

	private FamilyMember childMember(Integer childUserId) {
		User child = User.builder()
			.loginId("child" + childUserId)
			.password("pw")
			.name("아이")
			.email("child" + childUserId + "@example.com")
			.role(UserRole.CHILD)
			.build();
		ReflectionTestUtils.setField(child, "id", childUserId);
		return FamilyMember.builder().user(child).build();
	}

	@Test
	@DisplayName("등록 이벤트를 받으면 그 가족의 자녀 전원에게 PURCHASE 알림을 보낸다")
	void onProductCreated_notifiesAllChildrenInFamily() {
		given(familyMemberRepository.findAllByFamilyIdAndRole(FAMILY_ID, UserRole.CHILD))
			.willReturn(List.of(childMember(2), childMember(3)));
		StoreProductCreatedEvent event = new StoreProductCreatedEvent(5, FAMILY_ID, "자전거", "설명",
			StoreProductCategory.TOY);

		listener.onProductCreated(event);

		then(pushNotificationService).should(times(1))
			.notify(eq(2), eq(NotificationType.PURCHASE), any(), any(), eq(5));
		then(pushNotificationService).should(times(1))
			.notify(eq(3), eq(NotificationType.PURCHASE), any(), any(), eq(5));
	}

	@Test
	@DisplayName("가족에 자녀가 없으면 알림을 보내지 않는다")
	void onProductCreated_noChildren_doesNothing() {
		given(familyMemberRepository.findAllByFamilyIdAndRole(FAMILY_ID, UserRole.CHILD)).willReturn(List.of());
		StoreProductCreatedEvent event = new StoreProductCreatedEvent(5, FAMILY_ID, "자전거", "설명",
			StoreProductCategory.TOY);

		listener.onProductCreated(event);

		then(pushNotificationService).should(org.mockito.Mockito.never())
			.notify(any(), any(), any(), any(), any());
	}
}
