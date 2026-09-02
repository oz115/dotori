package com.bana.dotori.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.bana.dotori.user.entity.UserRole;

import io.jsonwebtoken.JwtException;

class JwtTokenProviderTest {

	private static final String SECRET = Base64.getEncoder()
		.encodeToString("dotori-test-secret-key-for-jwt-provider-unit-test".getBytes(StandardCharsets.UTF_8));

	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 1800, 1209600);

	@Test
	@DisplayName("Access Token으로는 인증 정보가 생성된다")
	void getAuthentication_accessToken() {
		String accessToken = jwtTokenProvider.createAccessToken(1, UserRole.PARENT);

		Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);

		assertThat(authentication.getPrincipal()).isEqualTo(1);
		assertThat(authentication.getAuthorities())
			.extracting(authority -> authority.getAuthority())
			.containsExactly("ROLE_PARENT");
	}

	@Test
	@DisplayName("Refresh Token으로 인증을 시도하면 거부된다")
	void getAuthentication_rejectsRefreshToken() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1);

		assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(refreshToken))
			.isInstanceOf(JwtException.class);
	}

	@Test
	@DisplayName("만료된 Access Token으로 인증을 시도하면 거부된다")
	void getAuthentication_rejectsExpiredToken() {
		JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1, 1209600);
		String expiredToken = expiredProvider.createAccessToken(1, UserRole.PARENT);

		assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(expiredToken))
			.isInstanceOf(JwtException.class);
	}

	@Test
	@DisplayName("서명이 다른 토큰으로 인증을 시도하면 거부된다")
	void getAuthentication_rejectsForgedToken() {
		String otherSecret = Base64.getEncoder()
			.encodeToString("another-secret-key-that-attacker-would-be-using!".getBytes(StandardCharsets.UTF_8));
		JwtTokenProvider forger = new JwtTokenProvider(otherSecret, 1800, 1209600);
		String forgedToken = forger.createAccessToken(1, UserRole.PARENT);

		assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(forgedToken))
			.isInstanceOf(JwtException.class);
	}
}
