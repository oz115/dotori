package com.bana.dotori.auth.service;

import java.time.OffsetDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResult;
import com.bana.dotori.auth.dto.TokenRefreshResult;
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
	public LoginResult login(LoginRequest request) {
		User user = userRepository.findByLoginId(request.loginId())
			.filter(found -> found.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
		String refreshToken = issueRefreshToken(user);

		return authMapper.toLoginResult(accessToken, refreshToken,
			jwtTokenProvider.getAccessTokenValiditySeconds(), user);
	}

	@Override
	@Transactional
	public TokenRefreshResult refresh(String refreshToken) {
		RefreshToken stored = findUsableToken(refreshToken, ErrorCode.INVALID_REFRESH_TOKEN);
		User user = stored.getUser();
		if (user.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		stored.revoke();
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
		String newRefreshToken = issueRefreshToken(user);

		return authMapper.toTokenRefreshResult(accessToken, newRefreshToken,
			jwtTokenProvider.getAccessTokenValiditySeconds());
	}

	@Override
	@Transactional
	public void logout(Integer userId, String refreshToken) {
		RefreshToken stored = findUsableToken(refreshToken, ErrorCode.UNAUTHORIZED);
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
