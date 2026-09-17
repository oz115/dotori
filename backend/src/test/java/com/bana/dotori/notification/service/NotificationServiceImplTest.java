package com.bana.dotori.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.notification.dto.NotificationItem;
import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.dto.NotificationReadAllResponse;
import com.bana.dotori.notification.dto.NotificationReadResponse;
import com.bana.dotori.notification.dto.NotificationUnreadCountResponse;
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

	@Test
	@DisplayName("본인 알림이면 상세를 반환한다")
	void getNotification_success() {
		Notification notification = notification(1, NotificationType.CASHOUT, false);
		given(notificationRepository.findById(1)).willReturn(Optional.of(notification));

		NotificationItem item = notificationService.getNotification(USER_ID, 1);

		assertThat(item.notificationId()).isEqualTo(1);
		assertThat(item.type()).isEqualTo(NotificationType.CASHOUT);
		assertThat(item.isRead()).isFalse();
	}

	@Test
	@DisplayName("알림이 없으면 NOTIFICATION_NOT_FOUND 예외가 발생한다")
	void getNotification_notFound() {
		given(notificationRepository.findById(999)).willReturn(Optional.empty());

		assertThatThrownBy(() -> notificationService.getNotification(USER_ID, 999))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
	}

	@Test
	@DisplayName("다른 사용자의 알림이면 FORBIDDEN 예외가 발생한다")
	void getNotification_otherUsersNotification() {
		Notification notification = notification(1, NotificationType.CASHOUT, false);
		given(notificationRepository.findById(1)).willReturn(Optional.of(notification));

		assertThatThrownBy(() -> notificationService.getNotification(999, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("본인 알림을 읽음 처리하면 isRead가 true로 바뀐다")
	void markAsRead_success() {
		Notification notification = notification(1, NotificationType.CASHOUT, false);
		given(notificationRepository.findById(1)).willReturn(Optional.of(notification));

		NotificationReadResponse response = notificationService.markAsRead(USER_ID, 1);

		assertThat(response.notificationId()).isEqualTo(1);
		assertThat(response.isRead()).isTrue();
		assertThat(notification.isRead()).isTrue();
	}

	@Test
	@DisplayName("이미 읽은 알림을 다시 읽음 처리해도 그대로 성공한다(멱등)")
	void markAsRead_alreadyRead_staysSuccessful() {
		Notification notification = notification(1, NotificationType.CASHOUT, true);
		given(notificationRepository.findById(1)).willReturn(Optional.of(notification));

		NotificationReadResponse response = notificationService.markAsRead(USER_ID, 1);

		assertThat(response.isRead()).isTrue();
	}

	@Test
	@DisplayName("읽음 처리 대상 알림이 없으면 NOTIFICATION_NOT_FOUND 예외가 발생한다")
	void markAsRead_notFound() {
		given(notificationRepository.findById(999)).willReturn(Optional.empty());

		assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, 999))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
	}

	@Test
	@DisplayName("다른 사용자의 알림을 읽음 처리하려 하면 FORBIDDEN 예외가 발생한다")
	void markAsRead_otherUsersNotification() {
		Notification notification = notification(1, NotificationType.CASHOUT, false);
		given(notificationRepository.findById(1)).willReturn(Optional.of(notification));

		assertThatThrownBy(() -> notificationService.markAsRead(999, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
		assertThat(notification.isRead()).isFalse();
	}

	@Test
	@DisplayName("전체 읽음 처리 시 리포지토리가 반환한 건수를 그대로 updatedCount에 담는다")
	void markAllAsRead_returnsUpdatedCount() {
		given(notificationRepository.markAllAsRead(USER_ID)).willReturn(3);

		NotificationReadAllResponse response = notificationService.markAllAsRead(USER_ID);

		assertThat(response.updatedCount()).isEqualTo(3);
	}

	@Test
	@DisplayName("안 읽은 알림이 없으면 updatedCount 0을 반환한다")
	void markAllAsRead_noUnread_returnsZero() {
		given(notificationRepository.markAllAsRead(USER_ID)).willReturn(0);

		NotificationReadAllResponse response = notificationService.markAllAsRead(USER_ID);

		assertThat(response.updatedCount()).isEqualTo(0);
	}

	@Test
	@DisplayName("본인 알림이면 삭제한다")
	void deleteNotification_success() {
		Notification notification = notification(1, NotificationType.CASHOUT, false);
		given(notificationRepository.findById(1)).willReturn(Optional.of(notification));

		notificationService.deleteNotification(USER_ID, 1);

		then(notificationRepository).should().delete(notification);
	}

	@Test
	@DisplayName("삭제 대상 알림이 없으면 NOTIFICATION_NOT_FOUND 예외가 발생한다")
	void deleteNotification_notFound() {
		given(notificationRepository.findById(999)).willReturn(Optional.empty());

		assertThatThrownBy(() -> notificationService.deleteNotification(USER_ID, 999))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
		then(notificationRepository).should(never()).delete(any(Notification.class));
	}

	@Test
	@DisplayName("다른 사용자의 알림을 삭제하려 하면 FORBIDDEN 예외가 발생하고 삭제되지 않는다")
	void deleteNotification_otherUsersNotification() {
		Notification notification = notification(1, NotificationType.CASHOUT, false);
		given(notificationRepository.findById(1)).willReturn(Optional.of(notification));

		assertThatThrownBy(() -> notificationService.deleteNotification(999, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
		then(notificationRepository).should(never()).delete(any(Notification.class));
	}

	@Test
	@DisplayName("안 읽은 알림 개수를 리포지토리가 반환한 값 그대로 담아 반환한다")
	void getUnreadCount_returnsRepositoryCount() {
		given(notificationRepository.count(any(Specification.class))).willReturn(3L);

		NotificationUnreadCountResponse response = notificationService.getUnreadCount(USER_ID);

		assertThat(response.unreadCount()).isEqualTo(3);
	}

	@Test
	@DisplayName("안 읽은 알림이 없으면 unreadCount 0을 반환한다")
	void getUnreadCount_noUnread_returnsZero() {
		given(notificationRepository.count(any(Specification.class))).willReturn(0L);

		NotificationUnreadCountResponse response = notificationService.getUnreadCount(USER_ID);

		assertThat(response.unreadCount()).isEqualTo(0);
	}
}
