package com.bana.dotori.wallet.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

/**
 * 지갑 알림의 수신자와 금액 표기를 고정한다 (S15P21A505-246).
 *
 * <p>돈이 움직이는 알림이라 방향이 뒤바뀌면 바로 사고다 — 환전 승인이 부모에게 가거나
 * 충전이 아이에게 가면 누구도 필요한 정보를 못 받는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletNotificationListenerTest {

	private static final Integer FAMILY_ID = 96;
	private static final Integer PARENT_ID = 246;
	private static final Integer CHILD_ID = 247;
	private static final Integer REQUEST_ID = 5;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private WalletNotificationListener listener;

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

	@Test
	@DisplayName("환전 요청은 부모에게 가고 아이 이름과 금액이 담긴다")
	void 환전_요청은_부모에게() {
		givenChildName("민준");

		listener.onCashoutRequested(new PointCashoutRequestedEvent(REQUEST_ID, PARENT_ID, CHILD_ID, 300));

		then(pushNotificationService).should()
			.notify(eq(PARENT_ID), eq(NotificationType.CASHOUT), any(), contains("민준"), eq(REQUEST_ID));
	}

	@Test
	@DisplayName("환전 승인은 아이에게 간다")
	void 환전_승인은_아이에게() {
		listener.onCashoutApproved(new PointCashoutApprovedEvent(REQUEST_ID, CHILD_ID, 300));

		then(pushNotificationService).should()
			.notify(eq(CHILD_ID), eq(NotificationType.CASHOUT), any(), contains("300"), eq(REQUEST_ID));
	}

	@Test
	@DisplayName("환전 거절은 아이에게 가고 도토리가 돌아왔음을 알린다")
	void 환전_거절은_아이에게() {
		// 요청할 때 빠져나간 도토리가 되돌아온다. 그 사실을 알리지 않으면 아이는
		// 잔액이 줄었다 늘어난 이유를 모른다.
		listener.onCashoutDeclined(new PointCashoutDeclinedEvent(REQUEST_ID, CHILD_ID, 300));

		then(pushNotificationService).should()
			.notify(eq(CHILD_ID), eq(NotificationType.CASHOUT), any(), contains("다시"), eq(REQUEST_ID));
	}

	@Test
	@DisplayName("충전은 부모 전원에게 가고 실제 입금액이 담긴다")
	void 충전은_부모에게() {
		givenChildName("민준");
		given(familyMemberRepository.findAllByFamilyIdAndRole(FAMILY_ID, UserRole.PARENT))
			.willReturn(List.of(parentMember(PARENT_ID)));

		listener.onCharged(new PointChargedEvent(FAMILY_ID, CHILD_ID, 5000));

		then(pushNotificationService).should()
			.notify(eq(PARENT_ID), eq(NotificationType.CHARGE), any(), contains("5000"), isNull());
	}

	@Test
	@DisplayName("지급은 아이에게 간다")
	void 지급은_아이에게() {
		listener.onGranted(new PointGrantedEvent(CHILD_ID, 1000));

		then(pushNotificationService).should()
			.notify(eq(CHILD_ID), eq(NotificationType.CHARGE), any(), contains("1000"), isNull());
	}

	@Test
	@DisplayName("아이를 못 찾아도 알림은 나간다")
	void 이름을_못_찾아도_보낸다() {
		given(userRepository.findById(CHILD_ID)).willReturn(Optional.empty());

		listener.onCashoutRequested(new PointCashoutRequestedEvent(REQUEST_ID, PARENT_ID, CHILD_ID, 300));

		then(pushNotificationService).should()
			.notify(eq(PARENT_ID), eq(NotificationType.CASHOUT), any(), contains("300"), eq(REQUEST_ID));
	}
}
