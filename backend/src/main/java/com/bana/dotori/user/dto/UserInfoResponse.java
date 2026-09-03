package com.bana.dotori.user.dto;

import java.time.OffsetDateTime;

import com.bana.dotori.user.entity.UserRole;

/**
 * MYPAGE-01 회원 정보 조회 응답.
 */
public record UserInfoResponse(
	Integer userId,
	String loginId,
	String name,
	String email,
	UserRole role,
	boolean emailVerified,
	OffsetDateTime createdAt
) {
}
