package com.bana.dotori.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.auth.dto.LoginRequest;
import com.bana.dotori.auth.dto.LoginResponse;
import com.bana.dotori.auth.dto.RefreshTokenRequest;
import com.bana.dotori.auth.dto.TokenRefreshResponse;
import com.bana.dotori.auth.service.AuthService;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

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
