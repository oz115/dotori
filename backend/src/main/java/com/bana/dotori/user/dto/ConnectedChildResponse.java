package com.bana.dotori.user.dto;

import java.time.OffsetDateTime;

/**
 * MYPAGE-06 응답의 자녀 한 명.
 */
public record ConnectedChildResponse(
	Integer userId,
	String name,
	OffsetDateTime joinedAt
) {
}
