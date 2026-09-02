package com.bana.dotori.wallet.controller;

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
import com.bana.dotori.wallet.dto.request.PointChargeRequest;
import com.bana.dotori.wallet.dto.request.PointGrantRequest;
import com.bana.dotori.wallet.dto.response.PointBalanceResponse;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.dto.response.PointTransactionResponse;
import com.bana.dotori.wallet.service.PointChargeService;
import com.bana.dotori.wallet.service.PointGrantService;
import com.bana.dotori.wallet.service.PointWalletQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 포인트 지갑 API.
 *
 * <p>충전은 로그인한 아이 본인만, 지급은 그 가족의 부모만 호출할 수 있다.
 * 경로에 childId 가 드러나 있어 검증이 없으면 남의 지갑을 조작할 수 있으므로,
 * 서비스로 넘기기 전에 컨트롤러에서 호출자를 먼저 확인한다.
 *
 * <p>조회(WLT-04)는 규칙이 다르다. 아이 본인뿐 아니라 그 가족의 부모도 볼 수 있어서,
 * 컨트롤러에서 본인 여부를 따지지 않고 판정을 {@link PointWalletQueryService} 에 맡긴다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/wallet")
public class PointWalletController {

	private final PointChargeService pointChargeService;
	private final PointGrantService pointGrantService;
	private final PointWalletQueryService pointWalletQueryService;

	/**
	 * WLT-04 보유 포인트 조회.
	 *
	 * <p>아이는 본인 지갑을, 부모는 같은 가족 자녀의 지갑을 조회할 수 있다.
	 * 지갑이 아직 없어도 오류가 아니라 잔액 0으로 응답한다.
	 */
	@GetMapping("/balance")
	public PointBalanceResponse getBalance(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId) {

		return pointWalletQueryService.getBalance(familyId, childId, loginUserId);
	}

	/**
	 * WLT-01 포인트 충전. 아이가 실제 금액을 부모 계좌로 보내고 같은 금액을 포인트로 받는다.
	 *
	 * @param idempotencyKey 같은 요청이 중복 전송돼도 한 번만 반영되도록 하는 키.
	 *                       네트워크 재시도가 이중 충전으로 이어지지 않게 하려면 클라이언트가 보내야 한다.
	 */
	@PostMapping("/charge")
	@ResponseStatus(HttpStatus.CREATED)
	public PointTransactionResponse charge(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody PointChargeRequest request) {

		// 충전은 아이 본인의 계좌에서 돈이 빠져나간다. 남이 대신 부를 수 있으면
		// 다른 사람 계좌에서 출금시키는 셈이 되므로 본인 확인이 필수다.
		if (!childId.equals(loginUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
					"본인의 지갑만 충전할 수 있습니다. loginUserId=" + loginUserId + ", childId=" + childId);
		}

		PointChangeResult result = pointChargeService.charge(
				familyId, childId, request.amount(), idempotencyKey);
		return PointTransactionResponse.from(result);
	}

	/**
	 * WLT-02 포인트 지급. 부모가 실제 송금 없이 포인트를 발행해 자녀에게 준다.
	 *
	 * <p>대상 자녀는 경로의 childId 를 쓴다. 본문에도 childId 가 올 수 있는데(명세상 존재),
	 * 두 값이 다르면 어느 쪽이 맞는지 알 수 없으므로 거부한다.
	 *
	 * <p>호출자가 그 가족의 부모인지는 서비스가 확인한다. 컨트롤러는 역할만 본다.
	 */
	@PostMapping("/grant")
	@ResponseStatus(HttpStatus.CREATED)
	public PointTransactionResponse grant(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody PointGrantRequest request) {

		if (request.conflictsWith(childId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST,
					"경로와 본문의 childId 가 다릅니다. path=" + childId + ", body=" + request.childId());
		}

		PointChangeResult result = pointGrantService.grant(
				loginUserId, familyId, childId, request.amount(), request.description(), idempotencyKey);
		return PointTransactionResponse.from(result);
	}
}
