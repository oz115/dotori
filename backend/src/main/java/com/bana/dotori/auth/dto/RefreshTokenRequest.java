package com.bana.dotori.auth.dto;

/**
 * Refresh Token 을 body 로 받는 요청(재발급·로그아웃) 공용 DTO.
 *
 * <p>누락 시 에러 코드가 엔드포인트마다 달라서(Bean Validation 400 대신)
 * 컨트롤러에서 직접 검사한다.
 */
public record RefreshTokenRequest(
	String refreshToken
) {
}
