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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * login_id·email 의 유일성은 부분 유니크 인덱스(UX_USERS_*_ACTIVE, deleted_at IS NULL)가
 * 보장한다. 탈퇴가 소프트 삭제라 살아있는 행끼리만 유일하면 되는데, JPA 의
 * uniqueConstraints 로는 부분 인덱스를 표현할 수 없어 스키마(01-schema.sql)에만 둔다.
 */
@Entity
@Table(name = "users")
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
		// 가입은 이메일 인증 통과(verificationToken 검증)가 전제라 생성 시점에 이미 인증된 상태다.
		// 이 값이 false 가 되는 건 나중에 이메일 변경 기능이 생겨 재인증을 기다릴 때뿐이다.
		this.emailVerified = true;
	}

	/**
	 * MYPAGE-02 회원 정보 수정. 지금 바꿀 수 있는 값은 이름뿐이다.
	 */
	public void changeName(String name) {
		this.name = name;
	}

	/**
	 * MYPAGE-04 비밀번호 변경. 인코딩된 값을 받는다.
	 */
	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	/**
	 * MYPAGE-05 회원 탈퇴. 행을 지우지 않고 시각만 남긴다.
	 *
	 * <p>가족 소속·포인트 거래 이력이 이 회원을 참조하고 있어 실제로 지우면 이력이 끊긴다.
	 * 대신 조회 쿼리들이 deleted_at 을 보고 걸러 낸다.
	 */
	public void withdraw() {
		this.deletedAt = OffsetDateTime.now();
	}
}
