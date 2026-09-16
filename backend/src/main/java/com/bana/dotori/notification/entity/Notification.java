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
 * 알림함에 남기는 알림 이력 (S15P21A505-228).
 *
 * <p>ref_id 는 알림 타입마다 가리키는 대상 테이블이 달라 FK 를 걸지 않는다 (스키마 주석 참고).
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private NotificationType type;

	@Column(name = "title", length = 100, nullable = false)
	private String title;

	@Column(name = "body", length = 255, nullable = false)
	private String body;

	@Column(name = "ref_id")
	private Integer refId;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Builder
	private Notification(User user, NotificationType type, String title, String body, Integer refId) {
		this.user = user;
		this.type = type;
		this.title = title;
		this.body = body;
		this.refId = refId;
		this.read = false;
	}
}
