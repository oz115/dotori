package com.bana.dotori.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.bana.dotori.auth.dto.PasswordResetRequest;
import com.bana.dotori.auth.dto.RefreshTokenRequest;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.auth.service.AuthService;
import com.bana.dotori.auth.service.EmailVerificationService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final EmailVerificationService emailVerificationService;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refresh(
		@RequestBody(required = false) RefreshTokenRequest request) {
		String refreshToken = extractRefreshToken(request, ErrorCode.INVALID_REFRESH_TOKEN);
		return ResponseEntity.ok(authService.refresh(refreshToken));
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

	@PostMapping("/password-reset")
	public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
		authService.resetPassword(request);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@AuthenticationPrincipal Integer userId,
		@RequestBody(required = false) RefreshTokenRequest request) {
		String refreshToken = extractRefreshToken(request, ErrorCode.UNAUTHORIZED);
		authService.logout(userId, refreshToken);
		return ResponseEntity.noContent().build();
	}

	private String extractRefreshToken(RefreshTokenRequest request, ErrorCode errorCode) {
		if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
			throw new BusinessException(errorCode);
		}
		return request.refreshToken();
	}
}
