package com.bana.dotori.notification.dto;

import com.bana.dotori.notification.entity.DeviceTokenPlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 디바이스 토큰 등록 요청 (NOTI-08).
 */
public record DeviceTokenRegisterRequest(
	@NotBlank @Size(max = 255) String fcmToken,
	@NotNull DeviceTokenPlatform platform
) {
}
