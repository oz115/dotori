package com.bana.dotori.chore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.service.ChoreCompletionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 집안일 완료 인증 API (CHORE-11·12). 둘 다 아이가 호출한다.
 *
 * <p>취소 경로에 choreId 가 없는 것은 노션 명세를 그대로 따른 것이다. 요청 ID 하나로
 * 행을 찾을 수 있고, 그 행이 호출자 것인지는 서비스가 확인한다.
 */
@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/chores")
@RequiredArgsConstructor
public class ChoreCompletionController {

	private final ChoreCompletionService choreCompletionService;

	@PostMapping("/{choreId}/completion-requests")
	public ResponseEntity<ChoreCompletionResponse> requestCompletion(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer choreId,
		@Valid @RequestBody ChoreCompletionCreateRequest request) {
		ChoreCompletionResponse response =
			choreCompletionService.requestCompletion(userId, familyId, childId, choreId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 행을 지우지 않고 status 를 CANCELLED 로 바꾼다 (CHORE-12).
	 * 취소 이력이 남아야 부모가 사라진 요청을 되짚을 수 있다.
	 */
	@DeleteMapping("/completion-requests/{requestId}")
	public ResponseEntity<Void> cancelCompletion(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer requestId) {
		choreCompletionService.cancelCompletion(userId, familyId, childId, requestId);
		return ResponseEntity.noContent().build();
	}
}
