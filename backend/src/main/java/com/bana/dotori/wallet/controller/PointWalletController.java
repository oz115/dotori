package com.bana.dotori.wallet.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.dto.request.PointCashoutRequestCreateRequest;
import com.bana.dotori.wallet.dto.request.PointChargeRequest;
import com.bana.dotori.wallet.dto.request.PointGrantRequest;
import com.bana.dotori.wallet.dto.response.PointBalanceResponse;
import com.bana.dotori.wallet.dto.response.PointCashoutRequestResponse;
import com.bana.dotori.wallet.dto.response.PointCashoutRequestResult;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.dto.response.PointTransactionHistoryResponse;
import com.bana.dotori.wallet.dto.response.PointTransactionResponse;
import com.bana.dotori.wallet.entity.PointCashoutRequestStatus;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointCashoutRequestService;
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
	private final PointCashoutRequestService pointCashoutRequestService;

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
	 * WLT-06 포인트 거래내역 조회.
	 *
	 * <p>조회 권한은 잔액과 같다. 아이는 본인, 부모는 같은 가족 자녀다.
	 *
	 * <p>{@code type} 과 {@code sort} 는 enum 으로 받는다. 지원하지 않는 값이 오면
	 * 스프링이 바인딩 단계에서 걸러 400 으로 응답하므로, 서비스까지 내려가지 않는다.
	 *
	 * <p><b>무한 스크롤은 커서로 넘긴다.</b> 첫 요청은 {@code cursor} 없이 부르고,
	 * 이후에는 응답의 {@code page.nextCursor} 를 그대로 실어 보낸다.
	 * 페이지 번호를 쓰면 스크롤 중에 새 거래가 앞에 끼어들었을 때 전체가 밀려
	 * 이미 본 항목이 다시 나온다.
	 *
	 * @param startDate 조회 시작일(포함). 생략하면 제한 없음
	 * @param endDate   조회 종료일(포함). 생략하면 제한 없음
	 * @param type      거래 사유 필터. 생략하면 전체
	 * @param sort      거래 시각 정렬. 기본은 최신순(DESC)
	 * @param cursor    이전 응답의 nextCursor. 생략하면 첫 묶음
	 * @param size      한 번에 가져올 건수. 기본 20, 최대 100
	 */
	@GetMapping("/transactions")
	public PointTransactionHistoryResponse getTransactions(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate,
			@RequestParam(required = false) PointTransactionReason type,
			@RequestParam(defaultValue = "DESC") Sort.Direction sort,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") int size) {

		return pointWalletQueryService.getTransactions(
				familyId, childId, loginUserId, startDate, endDate, type, sort, cursor, size);
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

	/**
	 * WLT-07 환전 요청. 자녀가 보유 포인트를 현금으로 바꿔 달라고 요청한다.
	 * 요청 즉시 포인트가 차감되며, 실제 송금은 부모가 승인해야 일어난다.
	 */
	@PostMapping("/cashout-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public PointCashoutRequestResponse createCashoutRequest(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody PointCashoutRequestCreateRequest request) {

		// 요청 즉시 본인 포인트가 빠진다. 남이 대신 부를 수 있으면 다른 사람 포인트를
		// 마음대로 차감시키는 셈이 되므로 충전과 같은 이유로 본인 확인이 필수다.
		if (!childId.equals(loginUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
					"본인의 포인트만 환전 요청할 수 있습니다. loginUserId=" + loginUserId + ", childId=" + childId);
		}

		PointCashoutRequestResult result = pointCashoutRequestService.request(
				familyId, childId, request.amount(), idempotencyKey);
		return PointCashoutRequestResponse.from(result);
	}

	/**
	 * WLT-07 환전 요청 목록 조회. 조회 권한은 잔액·거래내역과 같다 — 자녀 본인, 또는
	 * 같은 가족의 부모.
	 *
	 * @param status 상태 필터. 생략하면 전체
	 */
	@GetMapping("/cashout-requests")
	public List<PointCashoutRequestResponse> getCashoutRequests(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@RequestParam(required = false) PointCashoutRequestStatus status) {

		return pointCashoutRequestService.list(familyId, childId, loginUserId, status).stream()
				.map(PointCashoutRequestResponse::from)
				.toList();
	}

	/**
	 * WLT-07 환전 승인. 부모가 자녀의 환전 요청을 승인하면 부모 계좌에서 자녀 계좌로
	 * 실제 송금이 일어난다. 호출자가 그 가족의 부모인지는 서비스가 확인한다.
	 *
	 * <p>송금이 실패하면 서비스가 차감했던 포인트를 자동으로 환급한다. 그 경우도
	 * 여기서는 502 로 응답한다 — 클라이언트 입장에서는 "승인이 실패했다"이지,
	 * 포인트가 정상으로 돌아왔다는 사실까지 알 필요는 없다.
	 */
	@PostMapping("/cashout-requests/{cashoutRequestId}/approve")
	public PointCashoutRequestResponse approveCashoutRequest(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@PathVariable Integer cashoutRequestId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

		PointCashoutRequestResult result = pointCashoutRequestService.approve(
				cashoutRequestId, familyId, childId, loginUserId, idempotencyKey);

		if (result.status() == PointCashoutRequestStatus.FAILED) {
			throw new BusinessException(ErrorCode.BANK_TRANSFER_FAILED,
					"환전 승인 중 송금이 실패해 포인트를 환급했습니다. cashoutRequestId=" + cashoutRequestId);
		}
		return PointCashoutRequestResponse.from(result);
	}

	/**
	 * WLT-07 환전 요청 취소. 자녀가 자기 PENDING 요청을 직접 취소하면 차감했던 포인트를
	 * 즉시 돌려받는다. 부모가 거절하는 기능은 없다 — 취소는 요청을 만든 자녀만 할 수 있다.
	 */
	@PostMapping("/cashout-requests/{cashoutRequestId}/cancel")
	public PointCashoutRequestResponse cancelCashoutRequest(
			@AuthenticationPrincipal Integer loginUserId,
			@PathVariable Integer familyId,
			@PathVariable Integer childId,
			@PathVariable Integer cashoutRequestId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

		PointCashoutRequestResult result = pointCashoutRequestService.cancel(
				cashoutRequestId, familyId, childId, loginUserId, idempotencyKey);
		return PointCashoutRequestResponse.from(result);
	}
}
