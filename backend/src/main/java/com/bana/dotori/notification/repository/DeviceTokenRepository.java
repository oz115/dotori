package com.bana.dotori.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.notification.entity.DeviceToken;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

	Optional<DeviceToken> findByFcmToken(String fcmToken);

	Optional<DeviceToken> findByUser_IdAndFcmToken(Integer userId, String fcmToken);

	/**
	 * 한 사용자가 등록한 모든 기기의 토큰 (FCM 발송 대상 조회, S15P21A505-228).
	 */
	List<DeviceToken> findAllByUser_Id(Integer userId);
}
