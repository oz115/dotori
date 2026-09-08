package com.bana.dotori.auth.service;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.bana.dotori.auth.dto.FindIdRequest;
import com.bana.dotori.auth.dto.FindIdResponse;
import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.PasswordResetRequest;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.auth.entity.RefreshToken;
import com.bana.dotori.auth.mapper.AuthMapper;
import com.bana.dotori.auth.repository.RefreshTokenRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.global.util.PasswordPolicy;
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
	private final EmailVerificationCodeManager emailVerificationCodeManager;

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
	public TokenRefreshResponse refresh(String refreshToken) {
		RefreshToken stored = findUsableToken(refreshToken, ErrorCode.INVALID_REFRESH_TOKEN);
		User user = stored.getUser();
		if (user.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		stored.revoke();
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
		String newRefreshToken = issueRefreshToken(user);

		return authMapper.toTokenRefreshResponse(accessToken, newRefreshToken,
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

	@Override
	@Transactional
	public void revokeAllRefreshTokens(Integer userId) {
		// 폐기 시각을 찍는 규칙은 엔티티가 갖고 있다. 로그아웃·재발급과 같은 경로를 타게 둔다.
		refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(userId).forEach(RefreshToken::revoke);
	}

	@Override
	@Transactional(readOnly = true)
	public FindIdResponse findLoginId(FindIdRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		// 검증만 하고 소모하지 않는다 — 같은 토큰으로 이어서 비밀번호 재설정까지 할 수 있어야 한다.
		// 소모(1회용 처리)는 비밀번호 재설정 성공 시점에만 한다.
		emailVerificationCodeManager.requireValidToken(email, request.verificationToken());

		// 이름 뒤에 붙는 공백(모바일 키보드가 자주 만든다) 때문에 404가 나지 않게 다듬는다
		User user = userRepository.findByNameAndEmail(request.name().strip(), email)
			.filter(found -> found.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return new FindIdResponse(maskLoginId(user.getLoginId()));
	}

	/**
	 * 아이디를 마스킹한다. 앞 4자·뒤 2자만 남기고 가운데를 '*' 로 가린다 (예: dotori123 → doto***23).
	 * 6자 이하로 짧으면 앞 2자만 남긴다. 전부 노출하면 마스킹의 의미가 없어서다.
	 */
	private String maskLoginId(String loginId) {
		if (loginId.length() <= 2) {
			return loginId.charAt(0) + "*".repeat(loginId.length() - 1);
		}
		if (loginId.length() <= 6) {
			return loginId.substring(0, 2) + "*".repeat(loginId.length() - 2);
		}
		return loginId.substring(0, 4)
			+ "*".repeat(loginId.length() - 6)
			+ loginId.substring(loginId.length() - 2);
	}

	@Override
	@Transactional
	public void resetPassword(PasswordResetRequest request) {
		PasswordPolicy.validate(request.newPassword());

		String email = request.email().toLowerCase(Locale.ROOT);
		emailVerificationCodeManager.requireValidToken(email, request.verificationToken());

		User user = userRepository.findByLoginId(request.loginId())
			.filter(found -> found.getDeletedAt() == null)
			.filter(found -> found.getEmail().toLowerCase(Locale.ROOT).equals(email))
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		// 비밀번호가 바뀌면 기존 세션은 전부 끊는다 (탈취된 세션 차단)
		revokeAllRefreshTokens(user.getId());

		// verificationToken(1회용) 소모는 커밋이 확정된 뒤에만 한다. 커밋 전에 지우면
		// 커밋 실패(롤백) 시 비밀번호는 안 바뀌었는데 토큰만 사라져 인증을 처음부터 다시 해야 한다.
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					emailVerificationCodeManager.deleteToken(email);
				}
			});
		} else {
			emailVerificationCodeManager.deleteToken(email);
		}
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
