package com.bana.dotori.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.notification.dto.NotificationItem;
import com.bana.dotori.notification.dto.NotificationListResponse;
import com.bana.dotori.notification.dto.NotificationReadAllResponse;
import com.bana.dotori.notification.dto.NotificationReadResponse;
import com.bana.dotori.notification.dto.NotificationUnreadCountResponse;
import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<NotificationListResponse> getNotifications(@AuthenticationPrincipal Integer userId,
			@RequestParam(required = false) NotificationType type,
			@RequestParam(required = false) Boolean isRead,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(notificationService.getNotifications(userId, type, isRead, page, size));
	}

	@GetMapping("/{notificationId}")
	public ResponseEntity<NotificationItem> getNotification(@AuthenticationPrincipal Integer userId,
			@PathVariable Integer notificationId) {
		return ResponseEntity.ok(notificationService.getNotification(userId, notificationId));
	}

	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<NotificationReadResponse> markAsRead(@AuthenticationPrincipal Integer userId,
			@PathVariable Integer notificationId) {
		return ResponseEntity.ok(notificationService.markAsRead(userId, notificationId));
	}

	@PatchMapping("/read-all")
	public ResponseEntity<NotificationReadAllResponse> markAllAsRead(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(notificationService.markAllAsRead(userId));
	}

	@DeleteMapping("/{notificationId}")
	public ResponseEntity<Void> deleteNotification(@AuthenticationPrincipal Integer userId,
			@PathVariable Integer notificationId) {
		notificationService.deleteNotification(userId, notificationId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/unread-count")
	public ResponseEntity<NotificationUnreadCountResponse> getUnreadCount(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(notificationService.getUnreadCount(userId));
	}
}
