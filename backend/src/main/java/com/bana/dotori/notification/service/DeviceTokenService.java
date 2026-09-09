package com.bana.dotori.notification.service;

import com.bana.dotori.notification.dto.DeviceTokenRegisterRequest;
import com.bana.dotori.notification.dto.DeviceTokenRegisterResponse;

public interface DeviceTokenService {

	/**
	 * 디바이스 토큰을 등록한다 (NOTI-08). 이미 등록된 토큰이면 소유자를 요청 사용자로
	 * 갈아치우는 upsert 로 동작한다.
	 */
	DeviceTokenRegisterResponse register(Integer userId, DeviceTokenRegisterRequest request);

	/**
	 * 요청 사용자 소유의 디바이스 토큰을 해제(삭제)한다 (NOTI-09). 다른 사용자의 토큰은
	 * 조회 조건에 사용자를 포함해 애초에 대상이 되지 않는다.
	 */
	void unregister(Integer userId, String fcmToken);
}
