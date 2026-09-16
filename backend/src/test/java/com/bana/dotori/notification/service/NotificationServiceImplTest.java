package com.bana.dotori.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.entity.Notification;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.repository.NotificationRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	private static final Integer USER_ID = 1;

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	private User user(Integer id) {
		User user = User.builder()
			.loginId("dotori" + id)
			.password("encoded-password")
			.name("김도토")
			.email("dotori" + id + "@example.com")
			.role(UserRole.CHILD)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Notification notification(Integer id, NotificationType type, boolean read) {
		Notification notification = Notification.builder()
			.user(user(USER_ID))
			.type(type)
			.title(type + " 알림")
			.body("본문")
			.refId(10)
			.build();
		ReflectionTestUtils.setField(notification, "id", id);
		ReflectionTestUtils.setField(notification, "read", read);
		ReflectionTestUtils.setField(notification, "createdAt", OffsetDateTime.now());
		return notification;
	}

	@Test
	@DisplayName("조회 결과를 페이지 정보와 함께 매핑해 반환한다")
	void getNotifications_mapsPageContent() {
		List<Notification> content = List.of(
			notification(1, NotificationType.PURCHASE, false),
			notification(2, NotificationType.CHORE, true));
		given(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
			.willReturn(new PageImpl<>(content, PageRequest.of(0, 20), 2));
		given(notificationRepository.count(any(Specification.class))).willReturn(5L);

		NotificationListResponse response = notificationService.getNotifications(USER_ID, null, null, 0, 20);

		assertThat(response.notifications()).hasSize(2);
		assertThat(response.notifications().get(0).notificationId()).isEqualTo(1);
		assertThat(response.notifications().get(0).type()).isEqualTo(NotificationType.PURCHASE);
		assertThat(response.notifications().get(0).isRead()).isFalse();
		assertThat(response.notifications().get(1).isRead()).isTrue();
		assertThat(response.page()).isEqualTo(0);
		assertThat(response.totalPages()).isEqualTo(1);
		assertThat(response.unreadCount()).isEqualTo(5);
	}

	@Test
	@DisplayName("요청한 page/size 그대로 페이징 조건을 만든다")
	void getNotifications_buildsPageableFromArgs() {
		given(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of()));
		given(notificationRepository.count(any(Specification.class))).willReturn(0L);

		notificationService.getNotifications(USER_ID, null, null, 2, 10);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		then(notificationRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
	}

	@Test
	@DisplayName("unreadCount는 요청 필터(type/isRead)와 무관하게 항상 따로 조회한다")
	void getNotifications_unreadCountIgnoresRequestFilters() {
		given(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of()));
		given(notificationRepository.count(any(Specification.class))).willReturn(7L);

		NotificationListResponse response = notificationService.getNotifications(USER_ID, NotificationType.CASHOUT,
			true, 0, 20);

		assertThat(response.unreadCount()).isEqualTo(7);
		then(notificationRepository).should().count(any(Specification.class));
	}
}
