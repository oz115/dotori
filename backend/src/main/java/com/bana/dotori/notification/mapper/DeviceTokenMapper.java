package com.bana.dotori.notification.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.notification.dto.DeviceTokenRegisterRequest;
import com.bana.dotori.notification.dto.DeviceTokenRegisterResponse;
import com.bana.dotori.notification.entity.DeviceToken;
import com.bana.dotori.user.entity.User;

@Component
public class DeviceTokenMapper {

	public DeviceToken toEntity(DeviceTokenRegisterRequest request, User user) {
		return DeviceToken.builder()
			.user(user)
			.fcmToken(request.fcmToken())
			.platform(request.platform())
			.build();
	}

	public DeviceTokenRegisterResponse toRegisterResponse(DeviceToken deviceToken) {
		return new DeviceTokenRegisterResponse(deviceToken.getId(), deviceToken.getPlatform(),
			deviceToken.getLastUsedAt());
	}
}
