package com.bana.dotori.notification.entity;

/**
 * FCM 푸시를 수신할 디바이스 플랫폼.
 *
 * <p>DB의 device_tokens_platform enum 타입과 값이 같아야 한다.
 */
public enum DeviceTokenPlatform {
	ANDROID,
	IOS
}
