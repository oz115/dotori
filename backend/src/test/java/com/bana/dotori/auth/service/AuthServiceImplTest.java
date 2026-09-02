package com.bana.dotori.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Spy
	private AuthMapper authMapper;

	@InjectMocks
	private AuthServiceImpl authService;

	private User user(Integer id) {
		User user = User.builder()
			.loginId("dotori123")
			.password("encoded-password")
			.name("김도토")
			.email("dotori@example.com")
			.role(UserRole.PARENT)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private RefreshToken refreshToken(User user) {
		return RefreshToken.builder()
			.user(user)
			.tokenHash("stored-hash")
			.expiresAt(OffsetDateTime.now().plusDays(14))
			.build();
	}

	@Test
	@DisplayName("로그인 성공 시 토큰과 유저 요약 정보를 반환하고 Refresh Token을 저장한다")
	void login_success() {
		User user = user(1);
		given(userRepository.findByLoginId("dotori123")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("P@ssw0rd!", "encoded-password")).willReturn(true);
		given(jwtTokenProvider.createAccessToken(1, UserRole.PARENT)).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(1)).willReturn("refresh-token");
		given(jwtTokenProvider.hashToken("refresh-token")).willReturn("refresh-hash");
		given(jwtTokenProvider.getAccessTokenValiditySeconds()).willReturn(1800L);

		LoginResult result = authService.login(new LoginRequest("dotori123", "P@ssw0rd!"));

		assertThat(result.refreshToken()).isEqualTo("refresh-token");
		assertThat(result.response().accessToken()).isEqualTo("access-token");
		assertThat(result.response().tokenType()).isEqualTo("Bearer");
		assertThat(result.response().expiresIn()).isEqualTo(1800L);
		assertThat(result.response().user().userId()).isEqualTo(1);
		assertThat(result.response().user().role()).isEqualTo(UserRole.PARENT);
		verify(refreshTokenRepository).save(argThat(saved -> "refresh-hash".equals(saved.getTokenHash())));
	}

	@Test
	@DisplayName("존재하지 않는 아이디면 INVALID_CREDENTIALS 예외가 발생한다")
	void login_unknownLoginId() {
		given(userRepository.findByLoginId("nobody")).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "P@ssw0rd!")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	@DisplayName("비밀번호가 일치하지 않으면 INVALID_CREDENTIALS 예외가 발생한다")
	void login_wrongPassword() {
		given(userRepository.findByLoginId("dotori123")).willReturn(Optional.of(user(1)));
		given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("dotori123", "wrong")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	@DisplayName("재발급 성공 시 기존 토큰을 폐기하고 새 토큰을 발급한다")
	void refresh_success() {
		User user = user(1);
		RefreshToken stored = refreshToken(user);
		given(jwtTokenProvider.hashToken("old-refresh")).willReturn("stored-hash");
		given(jwtTokenProvider.hashToken("new-refresh")).willReturn("new-hash");
		given(refreshTokenRepository.findByTokenHash("stored-hash")).willReturn(Optional.of(stored));
		given(jwtTokenProvider.createAccessToken(1, UserRole.PARENT)).willReturn("new-access");
		given(jwtTokenProvider.createRefreshToken(1)).willReturn("new-refresh");
		given(jwtTokenProvider.getAccessTokenValiditySeconds()).willReturn(1800L);

		TokenRefreshResult result = authService.refresh("old-refresh");

		assertThat(result.response().accessToken()).isEqualTo("new-access");
		assertThat(result.refreshToken()).isEqualTo("new-refresh");
		assertThat(stored.getRevokedAt()).isNotNull();
		verify(refreshTokenRepository).save(argThat(saved -> "new-hash".equals(saved.getTokenHash())));
	}

	@Test
	@DisplayName("저장되지 않은 Refresh Token이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
	void refresh_unknownToken() {
		given(jwtTokenProvider.hashToken("forged")).willReturn("forged-hash");
		given(refreshTokenRepository.findByTokenHash("forged-hash")).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refresh("forged"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("이미 폐기된 Refresh Token이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
	void refresh_revokedToken() {
		RefreshToken stored = refreshToken(user(1));
		stored.revoke();
		given(jwtTokenProvider.hashToken("revoked")).willReturn("stored-hash");
		given(refreshTokenRepository.findByTokenHash("stored-hash")).willReturn(Optional.of(stored));

		assertThatThrownBy(() -> authService.refresh("revoked"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("로그아웃 성공 시 Refresh Token을 폐기한다")
	void logout_success() {
		RefreshToken stored = refreshToken(user(1));
		given(jwtTokenProvider.hashToken("refresh-token")).willReturn("stored-hash");
		given(refreshTokenRepository.findByTokenHash("stored-hash")).willReturn(Optional.of(stored));

		authService.logout(1, "refresh-token");

		assertThat(stored.getRevokedAt()).isNotNull();
	}

	@Test
	@DisplayName("다른 유저의 Refresh Token으로 로그아웃하면 UNAUTHORIZED 예외가 발생한다")
	void logout_otherUsersToken() {
		RefreshToken stored = refreshToken(user(2));
		given(jwtTokenProvider.hashToken("refresh-token")).willReturn("stored-hash");
		given(refreshTokenRepository.findByTokenHash("stored-hash")).willReturn(Optional.of(stored));

		assertThatThrownBy(() -> authService.logout(1, "refresh-token"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}
}
