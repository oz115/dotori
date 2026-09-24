package com.bana.dotori.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.user.dto.LoginIdCheckRequest;
import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping("/signup")
	public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
		SignupResponse response = userService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// 회원가입 화면의 중복 체크 버튼. 사용 가능하면 204, 사용 중이면 409 DUPLICATE_LOGIN_ID.
	@PostMapping("/login-id/check")
	public ResponseEntity<Void> checkLoginId(@Valid @RequestBody LoginIdCheckRequest request) {
		userService.checkLoginIdAvailable(request.loginId());
		return ResponseEntity.noContent().build();
	}
}
