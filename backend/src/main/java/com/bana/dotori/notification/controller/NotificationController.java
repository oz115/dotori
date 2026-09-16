package com.bana.dotori.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.notification.dto.NotificationListResponse;
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
}
