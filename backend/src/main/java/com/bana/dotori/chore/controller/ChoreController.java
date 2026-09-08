package com.bana.dotori.chore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.service.ChoreService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 집안일 API (CHORE).
 *
 * <p>경로에 familyId 가 없다. 가족은 인증 주체에서 찾는다 —
 * 노션 명세가 /api/v1/chores 로 정의돼 있고, 어차피 자기 가족 것만 다룰 수 있다.
 */
@RestController
@RequestMapping("/api/v1/chores")
@RequiredArgsConstructor
public class ChoreController {

	private final ChoreService choreService;

	@PostMapping
	public ResponseEntity<ChoreDetailResponse> createChore(@AuthenticationPrincipal Integer userId,
		@Valid @RequestBody ChoreCreateRequest request) {
		ChoreDetailResponse response = choreService.createChore(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
