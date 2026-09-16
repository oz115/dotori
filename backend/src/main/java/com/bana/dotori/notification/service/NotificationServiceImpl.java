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
import com.bana.dotori.notification.dto.NotificationItem;
import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.dto.NotificationReadAllResponse;
import com.bana.dotori.notification.dto.NotificationReadResponse;
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
		Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
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
