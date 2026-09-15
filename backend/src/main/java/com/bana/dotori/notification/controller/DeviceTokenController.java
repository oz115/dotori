package com.bana.dotori.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.notification.dto.DeviceTokenRegisterRequest;
import com.bana.dotori.notification.dto.DeviceTokenRegisterResponse;
import com.bana.dotori.notification.service.DeviceTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

	private final DeviceTokenService deviceTokenService;

	@PostMapping
	public ResponseEntity<DeviceTokenRegisterResponse> register(@AuthenticationPrincipal Integer userId,
			@Valid @RequestBody DeviceTokenRegisterRequest request) {
		return ResponseEntity.ok(deviceTokenService.register(userId, request));
	}

	@DeleteMapping
	public ResponseEntity<Void> unregister(@AuthenticationPrincipal Integer userId,
			@RequestParam(required = false) String fcmToken) {
		deviceTokenService.unregister(userId, fcmToken);
		return ResponseEntity.noContent().build();
	}
}
