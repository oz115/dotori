package com.bana.dotori.chore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.chore.dto.ChoreCreateRequest;
import com.bana.dotori.chore.dto.ChoreDetailResponse;
import com.bana.dotori.chore.dto.ChoreListResponse;
import com.bana.dotori.chore.dto.ChoreUpdateRequest;
import com.bana.dotori.chore.service.ChoreService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 집안일 API (CHORE).
 *
 * <p>경로가 가족과 아이를 함께 지목한다. 집안일은 언제나 특정 아이에게 배정되는 것이라
 * 가족 단위로만 묶는 것보다 이쪽이 명세와 맞고, 상점 API 와도 형태가 같다.
 *
 * <p>경로의 familyId·childId 를 그대로 믿지 않는다. 서비스에서 호출자가 그 가족의
 * 구성원인지, 그 아이가 그 가족의 자녀인지 매번 확인한다.
 */
@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/chores")
@RequiredArgsConstructor
public class ChoreController {

	private final ChoreService choreService;

	@GetMapping
	public ResponseEntity<ChoreListResponse> getChores(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId) {
		return ResponseEntity.ok(choreService.getChores(userId, familyId, childId));
	}

	@GetMapping("/{choreId}")
	public ResponseEntity<ChoreDetailResponse> getChore(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer choreId) {
		return ResponseEntity.ok(choreService.getChore(userId, familyId, childId, choreId));
	}

	@PostMapping
	public ResponseEntity<ChoreDetailResponse> createChore(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId,
		@Valid @RequestBody ChoreCreateRequest request) {
		ChoreDetailResponse response = choreService.createChore(userId, familyId, childId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PatchMapping("/{choreId}")
	public ResponseEntity<ChoreDetailResponse> updateChore(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer choreId,
		@Valid @RequestBody ChoreUpdateRequest request) {
		return ResponseEntity.ok(choreService.updateChore(userId, familyId, childId, choreId, request));
	}

	/**
	 * 물리 삭제가 아니라 deleted_at 을 채우는 소프트 삭제다 (CHORE-06).
	 * 완료 이력이 choreId 를 참조하므로 행을 지우면 이미 지급한 보상의 근거가 사라진다.
	 */
	@DeleteMapping("/{choreId}")
	public ResponseEntity<Void> deleteChore(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer choreId) {
		choreService.deleteChore(userId, familyId, childId, choreId);
		return ResponseEntity.noContent().build();
	}
}
