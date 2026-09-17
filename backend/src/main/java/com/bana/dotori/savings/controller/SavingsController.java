package com.bana.dotori.savings.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.savings.dto.request.SavingsContributeRequest;
import com.bana.dotori.savings.dto.request.SavingsSubscribeRequest;
import com.bana.dotori.savings.dto.response.SavingsExpectedReturnResponse;
import com.bana.dotori.savings.dto.response.SavingsProductResponse;
import com.bana.dotori.savings.dto.response.SavingsSubscriptionResponse;
import com.bana.dotori.savings.dto.response.SavingsSummaryResponse;
import com.bana.dotori.savings.service.SavingsService;

import lombok.RequiredArgsConstructor;

/**
 * 예적금 API. 조회는 아이 본인과 그 가족의 부모 모두 가능하지만, 포인트가 움직이는 요청
 * (가입·납입·수령·해지)은 아이 본인만 할 수 있다 — 부모는 개입하지 않는 완전 자동 상품이라
 * 승인·대리 요청이 없고, 남이 대신 부를 수 있으면 다른 사람 포인트를 마음대로 움직이는 셈이 된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/savings")
public class SavingsController {

	private final SavingsService savingsService;

	@GetMapping("/products")
	public List<SavingsProductResponse> getProducts(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId) {

		return savingsService.listProducts(loginUserId, familyId, childId);
	}

	/** SAV-02 상품 상세. 목록과 같은 내용이지만 화면이 상품 하나만 열 때 쓴다 */
	@GetMapping("/products/{savingsProductId}")
	public SavingsProductResponse getProduct(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId,
		@PathVariable Integer savingsProductId) {

		return savingsService.getProduct(loginUserId, familyId, childId, savingsProductId);
	}

	@PostMapping("/subscriptions")
	@ResponseStatus(HttpStatus.CREATED)
	public SavingsSubscriptionResponse subscribe(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId,
		@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
		@Valid @RequestBody SavingsSubscribeRequest request) {

		requireSelf(loginUserId, childId, "가입");

		return savingsService.subscribe(loginUserId, familyId, childId, request, idempotencyKey);
	}

	@PostMapping("/subscriptions/{savingsSubscriptionId}/contributions")
	@ResponseStatus(HttpStatus.CREATED)
	public SavingsSubscriptionResponse contribute(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId,
		@PathVariable Integer savingsSubscriptionId,
		@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
		@Valid @RequestBody SavingsContributeRequest request) {

		requireSelf(loginUserId, childId, "납입");

		return savingsService.contribute(loginUserId, familyId, childId, savingsSubscriptionId, request,
			idempotencyKey);
	}

	/** 만기 수령. 원금과 쌓인 이자가 지갑으로 들어온다 */
	@PostMapping("/subscriptions/{savingsSubscriptionId}/receipt")
	public SavingsSubscriptionResponse receive(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId,
		@PathVariable Integer savingsSubscriptionId,
		@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

		requireSelf(loginUserId, childId, "수령");

		return savingsService.receive(loginUserId, familyId, childId, savingsSubscriptionId, idempotencyKey);
	}

	/** 중도 해지. 납입 원금만 돌려받고 쌓인 이자는 잃는다 */
	@PostMapping("/subscriptions/{savingsSubscriptionId}/termination")
	public SavingsSubscriptionResponse terminate(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId,
		@PathVariable Integer savingsSubscriptionId,
		@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

		requireSelf(loginUserId, childId, "해지");

		return savingsService.terminate(loginUserId, familyId, childId, savingsSubscriptionId, idempotencyKey);
	}

	@GetMapping("/subscriptions")
	public List<SavingsSubscriptionResponse> getSubscriptions(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId) {

		return savingsService.listSubscriptions(loginUserId, familyId, childId);
	}

	@GetMapping("/subscriptions/{savingsSubscriptionId}")
	public SavingsSubscriptionResponse getSubscription(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId,
		@PathVariable Integer savingsSubscriptionId) {

		return savingsService.getSubscription(loginUserId, familyId, childId, savingsSubscriptionId);
	}

	/** 부모 홈 "자녀 예적금" 카드 요약. 조회라 부모도 볼 수 있다 */
	@GetMapping("/summary")
	public SavingsSummaryResponse getSummary(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId) {

		return savingsService.getSummary(loginUserId, familyId, childId);
	}

	/** SAV-05 만기 예상 수령액과 지금 해지 시 수령액. 조회라 부모도 볼 수 있다 */
	@GetMapping("/subscriptions/{savingsSubscriptionId}/expected-return")
	public SavingsExpectedReturnResponse getExpectedReturn(
		@AuthenticationPrincipal Integer loginUserId,
		@PathVariable Integer familyId,
		@PathVariable Integer childId,
		@PathVariable Integer savingsSubscriptionId) {

		return savingsService.getExpectedReturn(loginUserId, familyId, childId, savingsSubscriptionId);
	}

	/**
	 * 포인트가 움직이는 요청은 본인 확인을 먼저 한다. 충전·환전과 같은 이유다 —
	 * 서비스의 접근 검증은 "같은 가족인가"까지만 보므로 부모가 아이 대신 부르는 것을 막지 못한다.
	 */
	private void requireSelf(Integer loginUserId, Integer childId, String action) {
		if (!childId.equals(loginUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
				"본인만 예적금 " + action + "을(를) 할 수 있습니다. loginUserId=" + loginUserId + ", childId=" + childId);
		}
	}
}
