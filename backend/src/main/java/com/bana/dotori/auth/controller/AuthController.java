package com.bana.dotori.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.auth.dto.EmailVerificationConfirmRequest;
import com.bana.dotori.auth.dto.EmailVerificationConfirmResponse;
import com.bana.dotori.auth.dto.EmailVerificationSendRequest;
import com.bana.dotori.auth.dto.FindIdRequest;
import com.bana.dotori.auth.dto.FindIdResponse;
import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.LoginResult;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.auth.dto.TokenRefreshResult;
import com.bana.dotori.auth.service.AuthService;
import com.bana.dotori.auth.service.EmailVerificationService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.RefreshTokenCookieProvider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final EmailVerificationService emailVerificationService;
	private final RefreshTokenCookieProvider refreshTokenCookieProvider;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		LoginResult result = authService.login(request);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(result.refreshToken()).toString())
			.body(result.response());
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refresh(
		@CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		TokenRefreshResult result = authService.refresh(refreshToken);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(result.refreshToken()).toString())
			.body(result.response());
	}

	@PostMapping("/email-verification/send")
	public ResponseEntity<Void> sendEmailVerificationCode(
		@Valid @RequestBody EmailVerificationSendRequest request) {
		emailVerificationService.sendCode(request.email());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/email-verification/confirm")
	public ResponseEntity<EmailVerificationConfirmResponse> confirmEmailVerificationCode(
		@Valid @RequestBody EmailVerificationConfirmRequest request) {
		String verificationToken = emailVerificationService.confirmCode(request.email(), request.code());
		return ResponseEntity.ok(new EmailVerificationConfirmResponse(true, verificationToken));
	}

	@PostMapping("/find-id")
	public ResponseEntity<FindIdResponse> findId(@Valid @RequestBody FindIdRequest request) {
		return ResponseEntity.ok(authService.findLoginId(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@AuthenticationPrincipal Integer userId,
		@CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		authService.logout(userId, refreshToken);
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
			.build();
	}
}
