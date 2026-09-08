package com.bana.dotori.chore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;
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

	/**
	 * childId 는 부모가 특정 자녀만 보려고 넘기는 필터다.
	 * 아이가 넘겨도 자기 몫만 나온다 (서비스에서 무시한다).
	 */
	@GetMapping
	public ResponseEntity<ChoreListResponse> getChores(@AuthenticationPrincipal Integer userId,
		@RequestParam(required = false) Integer childId) {
		return ResponseEntity.ok(choreService.getChores(userId, childId));
	}

	@GetMapping("/{choreId}")
	public ResponseEntity<ChoreDetailResponse> getChore(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer choreId) {
		return ResponseEntity.ok(choreService.getChore(userId, choreId));
	}

	@PostMapping
	public ResponseEntity<ChoreDetailResponse> createChore(@AuthenticationPrincipal Integer userId,
		@Valid @RequestBody ChoreCreateRequest request) {
		ChoreDetailResponse response = choreService.createChore(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
