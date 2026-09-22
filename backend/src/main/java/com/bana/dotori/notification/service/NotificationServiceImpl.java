package com.bana.dotori.notification.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.notification.dto.NotificationDeleteAllResponse;
import com.bana.dotori.notification.dto.NotificationItem;
import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.dto.NotificationReadAllResponse;
import com.bana.dotori.notification.dto.NotificationReadResponse;
import com.bana.dotori.notification.dto.NotificationUnreadCountResponse;
import com.bana.dotori.notification.entity.Notification;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.repository.NotificationRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;

	@Override
	@Transactional(readOnly = true)
	public NotificationListResponse getNotifications(Integer userId, NotificationType type, Boolean isRead,
			int page, int size) {
		// 안 읽은 것이 먼저, 그 안에서 최신순. 최신순만으로 세우면 어제 안 읽은 알림이
		// 오늘 읽은 알림 밑으로 내려가, 처리할 것을 찾으려면 목록을 훑어야 한다.
		// 페이지 단위로 내려주므로 이 정렬은 서버가 해야 한다 - 클라이언트가 받아온 페이지
		// 안에서만 다시 세우면 다음 페이지의 미읽은 영영 위로 오지 못한다.
		Sort sort = Sort.by(Sort.Order.asc("read"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
		Page<Notification> result = notificationRepository.findAll(spec(userId, type, isRead),
			PageRequest.of(page, size, sort));

		// 배지용 unreadCount 는 이번 요청 필터와 무관하게 항상 본인 + 미읽 기준으로 따로 센다.
		long unreadCount = notificationRepository.count(spec(userId, null, false));

		List<NotificationItem> items = result.getContent().stream().map(this::toItem).toList();

		return new NotificationListResponse(items, result.getNumber(), result.getTotalPages(), unreadCount);
	}

	@Override
	@Transactional(readOnly = true)
	public NotificationItem getNotification(Integer userId, Integer notificationId) {
		return toItem(findOwnedOrThrow(userId, notificationId));
	}

	@Override
	@Transactional
	public NotificationReadResponse markAsRead(Integer userId, Integer notificationId) {
		Notification notification = findOwnedOrThrow(userId, notificationId);
		notification.markAsRead();
		return new NotificationReadResponse(notification.getId(), notification.isRead());
	}

	@Override
	@Transactional
	public NotificationReadAllResponse markAllAsRead(Integer userId) {
		int updatedCount = notificationRepository.markAllAsRead(userId);
		return new NotificationReadAllResponse(updatedCount);
	}

	@Override
	@Transactional
	public void deleteNotification(Integer userId, Integer notificationId) {
		notificationRepository.delete(findOwnedOrThrow(userId, notificationId));
	}

	@Override
	@Transactional
	public NotificationDeleteAllResponse deleteAllNotifications(Integer userId) {
		return new NotificationDeleteAllResponse(notificationRepository.deleteAllByUserId(userId));
	}

	@Override
	@Transactional(readOnly = true)
	public NotificationUnreadCountResponse getUnreadCount(Integer userId) {
		long unreadCount = notificationRepository.count(spec(userId, null, false));
		return new NotificationUnreadCountResponse(unreadCount);
	}

	private Notification findOwnedOrThrow(Integer userId, Integer notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
		if (!notification.getUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return notification;
	}

	private NotificationItem toItem(Notification n) {
		return new NotificationItem(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getRefId(), n.isRead(),
			n.getCreatedAt());
	}

	private Specification<Notification> spec(Integer userId, NotificationType type, Boolean isRead) {
		return (root, query, builder) -> {
			List<Predicate> conditions = new ArrayList<>();
			conditions.add(builder.equal(root.get("user").get("id"), userId));
			if (type != null) {
				conditions.add(builder.equal(root.get("type"), type));
			}
			if (isRead != null) {
				conditions.add(builder.equal(root.get("read"), isRead));
			}
			return builder.and(conditions.toArray(new Predicate[0]));
		};
	}
}
