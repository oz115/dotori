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

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.request.PointChargeRequest;
import com.bana.dotori.wallet.dto.request.PointGrantRequest;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.dto.response.PointTransactionResponse;
import com.bana.dotori.wallet.service.PointChargeService;
import com.bana.dotori.wallet.service.PointGrantService;

import lombok.RequiredArgsConstructor;

/**
 * 포인트 지갑 API.
 *
 * <p>TODO 인증이 붙으면(S15P21A505-19 계열) 아래 검증을 추가해야 한다.
 * 충전은 경로의 childId 가 로그인한 아이 본인인지, 지급은 호출자가 그 가족의 부모인지 확인해야 한다.
 * 지금은 인증 필터가 없어 "대상 아이가 그 가족 소속인지"까지만 검증한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/wallet")
public class PointWalletController {

	private final PointChargeService pointChargeService;
	private final PointGrantService pointGrantService;

	/**
	 * WLT-01 포인트 충전. 아이가 실제 금액을 부모 계좌로 보내고 같은 금액을 포인트로 받는다.
	 *
	 * @param idempotencyKey 같은 요청이 중복 전송돼도 한 번만 반영되도록 하는 키.
	 *                       네트워크 재시도가 이중 충전으로 이어지지 않게 하려면 클라이언트가 보내야 한다.
	 */
	@PostMapping("/charge")
	@ResponseStatus(HttpStatus.CREATED)
	public PointTransactionResponse charge(
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody PointChargeRequest request) {

		PointChangeResult result = pointChargeService.charge(
				familyId, childId, request.amount(), idempotencyKey);
		return PointTransactionResponse.from(result);
	}

	/**
	 * WLT-02 포인트 지급. 부모가 실제 송금 없이 포인트를 발행해 자녀에게 준다.
	 *
	 * <p>대상 자녀는 경로의 childId 를 쓴다. 본문에도 childId 가 올 수 있는데(명세상 존재),
	 * 두 값이 다르면 어느 쪽이 맞는지 알 수 없으므로 거부한다.
	 */
	@PostMapping("/grant")
	@ResponseStatus(HttpStatus.CREATED)
	public PointTransactionResponse grant(
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody PointGrantRequest request) {

		if (request.conflictsWith(childId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST,
					"경로와 본문의 childId 가 다릅니다. path=" + childId + ", body=" + request.childId());
		}

		PointChangeResult result = pointGrantService.grant(
				familyId, childId, request.amount(), request.description(), idempotencyKey);
		return PointTransactionResponse.from(result);
	}
}
