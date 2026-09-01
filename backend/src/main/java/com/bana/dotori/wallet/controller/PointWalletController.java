package com.bana.dotori.wallet.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.wallet.dto.request.PointChargeRequest;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.dto.response.PointChargeResponse;
import com.bana.dotori.wallet.service.PointChargeService;

import lombok.RequiredArgsConstructor;

/**
 * 포인트 지갑 API.
 *
 * <p>TODO 인증이 붙으면(S15P21A505-19 계열) 경로의 childId 가 실제 로그인한 아이 본인인지,
 * 그리고 그 아이가 familyId 소속인지 검증해야 한다. 지금은 인증 필터가 없어 검증을 걸 수 없다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/wallet")
public class PointWalletController {

	private final PointChargeService pointChargeService;

	/**
	 * WLT-01 포인트 충전. 아이가 실제 금액을 부모 계좌로 보내고 같은 금액을 포인트로 받는다.
	 *
	 * @param idempotencyKey 같은 요청이 중복 전송돼도 한 번만 반영되도록 하는 키.
	 *                       네트워크 재시도가 이중 충전으로 이어지지 않게 하려면 클라이언트가 보내야 한다.
	 */
	@PostMapping("/charge")
	@ResponseStatus(HttpStatus.CREATED)
	public PointChargeResponse charge(
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody PointChargeRequest request) {

		PointChangeResult result = pointChargeService.charge(
				familyId, childId, request.amount(), idempotencyKey);
		return PointChargeResponse.from(result);
	}
}
