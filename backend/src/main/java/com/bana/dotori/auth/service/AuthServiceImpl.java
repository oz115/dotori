package com.bana.dotori.auth.service;

import java.time.OffsetDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.LogoutRequest;
import com.bana.dotori.auth.dto.TokenRefreshRequest;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.auth.entity.RefreshToken;
import com.bana.dotori.auth.mapper.AuthMapper;
import com.bana.dotori.auth.repository.RefreshTokenRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthMapper authMapper;

	@Override
	@Transactional
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByLoginId(request.loginId())
			.filter(found -> found.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
		String refreshToken = issueRefreshToken(user);

		return authMapper.toLoginResponse(accessToken, refreshToken,
			jwtTokenProvider.getAccessTokenValiditySeconds(), user);
	}

	@Override
	@Transactional
	public TokenRefreshResponse refresh(TokenRefreshRequest request) {
		RefreshToken stored = findUsableToken(request.refreshToken(), ErrorCode.INVALID_REFRESH_TOKEN);
		User user = stored.getUser();
		if (user.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		stored.revoke();
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
		String refreshToken = issueRefreshToken(user);

		return authMapper.toTokenRefreshResponse(accessToken, refreshToken,
			jwtTokenProvider.getAccessTokenValiditySeconds());
	}

	@Override
	@Transactional
	public void logout(Integer userId, LogoutRequest request) {
		RefreshToken stored = findUsableToken(request.refreshToken(), ErrorCode.UNAUTHORIZED);
		if (!stored.getUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		stored.revoke();
	}

	private RefreshToken findUsableToken(String refreshToken, ErrorCode errorCode) {
		try {
			jwtTokenProvider.parseClaims(refreshToken);
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(errorCode);
		}
		return refreshTokenRepository.findByTokenHash(jwtTokenProvider.hashToken(refreshToken))
			.filter(RefreshToken::isUsable)
			.orElseThrow(() -> new BusinessException(errorCode));
	}

	private String issueRefreshToken(User user) {
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
		refreshTokenRepository.save(RefreshToken.builder()
			.user(user)
			.tokenHash(jwtTokenProvider.hashToken(refreshToken))
			.expiresAt(OffsetDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenValiditySeconds()))
			.build());
		return refreshToken;
	}
}
