package com.bana.dotori.chore.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.chore.dto.ChoreApprovalResponse;
import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.dto.ChoreCompletionHistoryResponse;
import com.bana.dotori.chore.dto.ChoreCompletionListResponse;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.dto.ChoreRejectionResponse;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;
import com.bana.dotori.chore.service.ChoreApprovalService;
import com.bana.dotori.chore.service.ChoreCompletionHistoryService;
import com.bana.dotori.chore.service.ChoreCompletionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 집안일 완료 인증 API (CHORE-03·07·08·09·10·11·12).
 *
 * <p>요청·취소(11·12)는 아이가, 대기함 조회(08)·승인(09)·거절(10)은 부모가 호출하고,
 * 완료 내역(03)은 둘 다 본다.
 * 경로는 같아도 역할이 다르므로 서비스가 매번 호출자의 역할을 확인한다.
 *
 * <p>취소 경로에 choreId 가 없는 것은 노션 명세를 그대로 따른 것이다. 요청 ID 하나로
 * 행을 찾을 수 있고, 그 행이 호출자 것인지는 서비스가 확인한다.
 */
@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/chores")
@RequiredArgsConstructor
public class ChoreCompletionController {

	private final ChoreCompletionService choreCompletionService;
	private final ChoreApprovalService choreApprovalService;
	private final ChoreCompletionHistoryService choreCompletionHistoryService;

	/**
	 * 부모 대기함 (CHORE-08).
	 *
	 * <p>status 를 안 보내면 PENDING 이다. 이 화면은 처리할 것을 보러 오는 곳이다.
	 * 없는 상태 이름이 오면 스프링의 열거형 변환이 실패해 400 이 나간다.
	 */
	@GetMapping("/completion-requests")
	public ResponseEntity<ChoreCompletionListResponse> getCompletionRequests(
		@AuthenticationPrincipal Integer userId, @PathVariable Integer familyId, @PathVariable Integer childId,
		@RequestParam(defaultValue = "PENDING") ChoreCompletionStatus status) {
		return ResponseEntity.ok(choreCompletionService.getCompletionRequests(userId, familyId, childId, status));
	}

	/**
	 * 완료 내역 (CHORE-03). 부모와 아이가 모두 본다.
	 *
	 * <p>기간을 안 보내면 최근 30일이다. 응답 금액은 지갑 원장에서 읽은 <b>실제 지급액</b>이라
	 * 부모가 나중에 보상을 고쳐도 과거 이력은 그대로 남는다.
	 */
	@GetMapping("/completions")
	public ResponseEntity<ChoreCompletionHistoryResponse> getCompletions(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ResponseEntity.ok(choreCompletionHistoryService.getCompletions(
			userId, familyId, childId, startDate, endDate));
	}

	@PostMapping("/{choreId}/completion-requests")
	public ResponseEntity<ChoreCompletionResponse> requestCompletion(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer choreId,
		@Valid @RequestBody ChoreCompletionCreateRequest request) {
		ChoreCompletionResponse response =
			choreCompletionService.requestCompletion(userId, familyId, childId, choreId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 부모가 승인하고 보상을 지급한다 (CHORE-09 + CHORE-07).
	 *
	 * <p>상태 변경과 포인트 지급이 한 트랜잭션이다. 조회가 아니라 상태를 바꾸는 동작이라
	 * PUT 이 아니라 POST 다 — 명세도 그렇게 정해져 있다.
	 */
	@PostMapping("/completion-requests/{requestId}/approve")
	public ResponseEntity<ChoreApprovalResponse> approveCompletion(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer requestId) {
		return ResponseEntity.ok(choreApprovalService.approve(userId, familyId, childId, requestId));
	}

	/**
	 * 부모가 거절한다 (CHORE-10).
	 *
	 * <p>본문이 없다. 명세에는 거절 사유(reason)가 있지만 담을 컬럼이 없고 사유가 향할
	 * 알림 도메인도 없어 팀 결정으로 뺐다 — 명세도 같이 고쳐야 한다.
	 *
	 * <p>거절해도 아이는 같은 주기에 다시 요청할 수 있다.
	 */
	@PostMapping("/completion-requests/{requestId}/reject")
	public ResponseEntity<ChoreRejectionResponse> rejectCompletion(@AuthenticationPrincipal Integer userId,
		@PathVariable Integer familyId, @PathVariable Integer childId, @PathVariable Integer requestId) {
		return ResponseEntity.ok(choreCompletionService.rejectCompletion(userId, familyId, childId, requestId));
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
