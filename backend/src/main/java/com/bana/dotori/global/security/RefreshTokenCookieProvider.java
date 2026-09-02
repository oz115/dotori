package com.bana.dotori.global.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieProvider {

	public static final String COOKIE_NAME = "refreshToken";

	// 쿠키가 인증 관련 엔드포인트(/refresh, /logout)로만 전송되도록 경로 제한
	private static final String COOKIE_PATH = "/api/v1/auth";
	private static final String SAME_SITE = "Lax";

	private final long refreshTokenValiditySeconds;
	private final boolean secure;

	public RefreshTokenCookieProvider(
		@Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds,
		@Value("${jwt.cookie-secure:false}") boolean secure
	) {
		this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
		this.secure = secure;
	}

	public ResponseCookie create(String refreshToken) {
		return ResponseCookie.from(COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(secure)
			.sameSite(SAME_SITE)
			.path(COOKIE_PATH)
			.maxAge(Duration.ofSeconds(refreshTokenValiditySeconds))
			.build();
	}

	public ResponseCookie expire() {
		return ResponseCookie.from(COOKIE_NAME, "")
			.httpOnly(true)
			.secure(secure)
			.sameSite(SAME_SITE)
			.path(COOKIE_PATH)
			.maxAge(0)
			.build();
	}
}
