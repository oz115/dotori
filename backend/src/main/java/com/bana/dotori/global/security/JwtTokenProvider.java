package com.bana.dotori.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.bana.dotori.user.entity.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;

@Component
public class JwtTokenProvider {

	private static final String TOKEN_TYPE_CLAIM = "typ";
	private static final String TOKEN_TYPE_ACCESS = "access";
	private static final String TOKEN_TYPE_REFRESH = "refresh";

	private final SecretKey secretKey;

	@Getter
	private final long accessTokenValiditySeconds;

	@Getter
	private final long refreshTokenValiditySeconds;

	public JwtTokenProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
		@Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds
	) {
		this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		this.accessTokenValiditySeconds = accessTokenValiditySeconds;
		this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
	}

	public String createAccessToken(Integer userId, UserRole role) {
		Date now = new Date();
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS)
			.claim("role", role.name())
			.issuedAt(now)
			.expiration(new Date(now.getTime() + accessTokenValiditySeconds * 1000))
			.signWith(secretKey)
			.compact();
	}

	public String createRefreshToken(Integer userId) {
		Date now = new Date();
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH)
			.id(UUID.randomUUID().toString())
			.issuedAt(now)
			.expiration(new Date(now.getTime() + refreshTokenValiditySeconds * 1000))
			.signWith(secretKey)
			.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public Authentication getAuthentication(String accessToken) {
		Claims claims = parseClaims(accessToken);
		// Refresh Token 등 다른 종류의 토큰으로 API 인증을 시도하는 것을 차단
		if (!TOKEN_TYPE_ACCESS.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
			throw new JwtException("Access Token이 아닙니다");
		}
		Integer userId = Integer.valueOf(claims.getSubject());
		String role = claims.get("role", String.class);
		return new UsernamePasswordAuthenticationToken(userId, null,
			List.of(new SimpleGrantedAuthority("ROLE_" + role)));
	}

	public String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256을 사용할 수 없는 환경입니다", e);
		}
	}
}
