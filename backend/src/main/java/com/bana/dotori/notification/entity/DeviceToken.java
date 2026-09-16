package com.bana.dotori.notification.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.bana.dotori.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 푸시 수신용 디바이스 토큰 (NOTI-08·09).
 *
 * <p>fcm_token 은 DB에서 전역 UNIQUE다. 기기가 로그아웃 후 다른 계정으로 로그인하면
 * 같은 토큰이 새 사용자 소유로 넘어와야 하므로, 등록 API는 이미 있는 토큰이면
 * {@link #reassignTo} 로 소유자를 갈아치우는 upsert 로 동작한다.
 */
@Entity
@Table(name = "device_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "device_token_id")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "fcm_token", nullable = false)
	private String fcmToken;

	@Enumerated(EnumType.STRING)
	@Column(name = "platform", nullable = false)
	private DeviceTokenPlatform platform;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "last_used_at", nullable = false)
	private OffsetDateTime lastUsedAt;

	@Builder
	private DeviceToken(User user, String fcmToken, DeviceTokenPlatform platform) {
		this.user = user;
		this.fcmToken = fcmToken;
		this.platform = platform;
		this.lastUsedAt = OffsetDateTime.now();
	}

	/**
	 * 등록 API가 이미 존재하는 토큰을 다시 받았을 때 호출한다(upsert). 소유자·플랫폼을
	 * 요청 기준으로 갈아치우고 last_used_at 을 갱신한다.
	 */
	public void reassignTo(User user, DeviceTokenPlatform platform) {
		this.user = user;
		this.platform = platform;
		this.lastUsedAt = OffsetDateTime.now();
	}
}
