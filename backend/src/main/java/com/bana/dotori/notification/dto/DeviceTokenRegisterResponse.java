package com.bana.dotori.notification.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.notification.entity.DeviceTokenPlatform;

public record DeviceTokenRegisterResponse(
	Integer deviceTokenId,
	DeviceTokenPlatform platform,
	OffsetDateTime registeredAt
) {
}
