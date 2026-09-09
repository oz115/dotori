package com.bana.dotori.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.notification.entity.DeviceToken;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

	Optional<DeviceToken> findByFcmToken(String fcmToken);
}
