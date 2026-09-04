package com.bana.dotori.user.dto;

import java.util.List;

/**
 * MYPAGE-06 연결된 자녀 조회 응답.
 */
public record ConnectedChildrenResponse(
	List<ConnectedChildResponse> children
) {
}
