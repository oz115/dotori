package com.bana.dotori.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.user.dto.UserInfoResponse;
import com.bana.dotori.user.dto.UserUpdateRequest;
import com.bana.dotori.user.dto.UserUpdateResponse;
import com.bana.dotori.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 마이페이지(MYPAGE) API.
 *
 * <p>회원가입은 인증 흐름에 속해 {@link UserController} 가 {@code /api/v1/auth} 아래에서 처리한다.
 * 여기서는 로그인한 사용자가 자기 계정을 다루는 {@code /api/v1/users} 경로만 담당한다.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class MyPageController {

	private final UserService userService;

	/**
	 * MYPAGE-01 회원 정보 조회.
	 *
	 * <p>조회 대상은 경로의 userId 이고, 본인 여부는 서비스가 확인한다.
	 */
	@GetMapping("/{userId}")
	public UserInfoResponse getUserInfo(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer userId) {

		return userService.getUserInfo(loginUserId, userId);
	}

	/**
	 * MYPAGE-02 회원 정보 수정.
	 *
	 * <p>부분 수정이라 본문에 담긴 필드만 바뀐다.
	 */
	@PatchMapping("/{userId}")
	public UserUpdateResponse updateUserInfo(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer userId,
			@Valid @RequestBody UserUpdateRequest request) {

		return userService.updateUserInfo(loginUserId, userId, request);
	}
}
