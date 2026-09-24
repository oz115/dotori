package com.bana.dotori.user.dto;

import java.time.OffsetDateTime;

/**
 * MYPAGE-02 회원 정보 수정 응답.
 */
public record UserUpdateResponse(
	Integer userId,
	String name,
	OffsetDateTime updatedAt
) {
}
