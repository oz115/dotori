package com.bana.dotori.user.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
	@UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"),
	@UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Integer id;

	@Column(name = "login_id", length = 50, nullable = false)
	private String loginId;

	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "name", length = 50, nullable = false)
	private String name;

	@Column(name = "email", nullable = false)
	private String email;

	// ERD상 role은 PG 네이티브 enum(users_role)이지만, ddl-auto:update가 재기동 시 enum 타입을
	// DROP CASCADE로 재생성해 컬럼이 유실되므로 스키마 관리가 스크립트/Flyway로 전환되기 전까지 varchar로 유지
	@Enumerated(EnumType.STRING)
	@Column(name = "role", length = 20, nullable = false)
	private UserRole role;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Builder
	private User(String loginId, String password, String name, String email, UserRole role) {
		this.loginId = loginId;
		this.password = password;
		this.name = name;
		this.email = email;
		this.role = role;
		this.emailVerified = false;
	}
}
