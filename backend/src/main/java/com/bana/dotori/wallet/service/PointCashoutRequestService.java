package com.bana.dotori.wallet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.util.PointExchangePolicy;
import com.bana.dotori.wallet.dto.response.PointCashoutRequestResult;
import com.bana.dotori.wallet.entity.PointCashoutRequest;
import com.bana.dotori.wallet.entity.PointCashoutRequestStatus;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;
import com.bana.dotori.wallet.repository.PointCashoutRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WLT-07 포인트 환전. 자녀가 요청하면 포인트를 즉시 차감하고, 부모가 승인하면
 * 부모 계좌에서 자녀 계좌로 같은 금액을 송금한다. 부모는 거절할 수도 있고,
 * 자녀는 자기 요청을 직접 취소할 수도 있다.
 *
 * <p><b>차감(요청 시)과 송금(승인 시)은 서로 다른 트랜잭션이다.</b> 요청 시점에 바로
 * 차감해야 자녀가 이미 쓴 포인트로 또 요청을 못 하고, 부모는 "이미 빠진 포인트"를 놓고
 * 승인 여부만 결정하면 된다. 대신 승인 중 송금이 실패해도, 요청 시점에 이미 커밋된 차감을
 * 트랜잭션 롤백으로 되돌릴 수 없다 — 그래서 {@link #approve} 는 송금 실패를 밖으로 던지는
 * 대신, 같은 트랜잭션 안에서 환급까지 끝내고 요청을 FAILED 로 남긴 뒤 정상 반환한다.
 * (실패를 밖으로 던지면 방금 커밋하려던 환급·FAILED 전이까지 함께 롤백돼 버린다.)
 * 승인 요청이 결국 실패했다는 사실은 반환된 결과의 {@code status}로 호출자가 판단한다.
 *
 * <p>{@code point_cashout_requests_status} 는 스키마에 {@code PROGRESS} 도 갖고 있지만
 * 지금 구현은 만들지 않는다(팀 결정). PENDING 은 바로 SUCCEED/FAILED/CANCELLED/DECLINED 로 끝난다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointCashoutRequestService {

	private static final String REQUEST_DESCRIPTION = "포인트 환전 요청";
	private static final String REFUND_DESCRIPTION = "환전 실패 환급";
	private static final String CANCEL_REFUND_DESCRIPTION = "환전 요청 취소 환급";
	private static final String DECLINE_REFUND_DESCRIPTION = "환전 요청 거절 환급";

	private final PointDeductionService pointDeductionService;
	private final PointCashoutRequestRepository pointCashoutRequestRepository;
	private final FamilyMembershipReader familyMembershipReader;
	private final BankTransferClient bankTransferClient;
	private final PointWalletQueryService pointWalletQueryService;

	/**
	 * @param pointAmount 현금으로 바꿀 포인트 수량. 항상 양수
	 * @throws BusinessException NOT_FAMILY_CHILD 해당 가족의 자녀가 아닌 경우,
	 *         ACCOUNT_NOT_LINKED 가족에 부모가 없는 경우, INSUFFICIENT_POINT 잔액이 부족한 경우
	 */
	@Transactional
	public PointCashoutRequestResult request(Integer familyId, Integer childId, int pointAmount,
			String idempotencyKey) {

		if (!familyMembershipReader.isChildOfFamily(familyId, childId)) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD,
					"해당 가족의 자녀가 아닙니다. familyId=" + familyId + ", childId=" + childId);
		}

		Integer parentId = familyMembershipReader.findParentUserId(familyId);
		if (parentId == null) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED,
					"가족의 부모를 찾을 수 없습니다. familyId=" + familyId);
		}

		// 잔액이 부족하면 여기서 INSUFFICIENT_POINT 로 끝난다 — 요청 행 자체가 생기지 않는다.
		pointDeductionService.deduct(childId, pointAmount, PointTransactionReason.CASHOUT,
				REQUEST_DESCRIPTION, idempotencyKey);

		PointCashoutRequest request = PointCashoutRequest.pending(familyId, childId, parentId, pointAmount);
		pointCashoutRequestRepository.save(request);

		log.info("환전 요청 접수. familyId={}, childId={}, parentId={}, pointAmount={}",
				familyId, childId, parentId, pointAmount);
		return PointCashoutRequestResult.from(request);
	}

	/**
	 * 부모가 그 가족의 자녀인지, 요청이 그 경로의 것인지, 아직 PENDING 인지 확인한 뒤
	 * 부모→자녀 송금을 시도한다. 송금이 성공하면 SUCCEED, 실패하면 환급 후 FAILED 를 돌려준다
	 * (예외를 던지지 않는다 — 클래스 설명 참고).
	 *
	 * @throws BusinessException FORBIDDEN 해당 가족의 부모가 아닌 경우,
	 *         CASHOUT_REQUEST_NOT_FOUND 요청이 없거나 경로와 다른 경우,
	 *         CASHOUT_REQUEST_ALREADY_PROCESSED 이미 처리된 요청인 경우
	 */
	@Transactional
	public PointCashoutRequestResult approve(Integer cashoutRequestId, Integer familyId, Integer childId,
			Integer loginParentId, String idempotencyKey) {

		if (!familyMembershipReader.isParentOfFamily(familyId, loginParentId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
					"해당 가족의 부모가 아닙니다. familyId=" + familyId + ", parentId=" + loginParentId);
		}

		PointCashoutRequest request = lockPendingRequest(cashoutRequestId, familyId, childId);

		int cashAmount = PointExchangePolicy.toWon(request.getPointAmount());
		try {
			BankTransferResult transfer = bankTransferClient.transferFromParentToChild(
					request.getParentId(), childId, cashAmount, idempotencyKey);
			if (transfer.transferredAmount() != cashAmount) {
				throw new BusinessException(ErrorCode.BANK_TRANSFER_FAILED,
						"요청 금액과 이체 금액이 다릅니다. cashoutRequestId=" + cashoutRequestId
								+ ", 요청=" + cashAmount + ", 이체=" + transfer.transferredAmount());
			}
			request.succeed();
			log.info("환전 승인 완료. cashoutRequestId={}, familyId={}, childId={}, parentId={}, pointAmount={}",
					cashoutRequestId, familyId, childId, request.getParentId(), request.getPointAmount());
		} catch (BusinessException e) {
			// 여기서 다시 던지면 방금 하려던 환급·FAILED 전이까지 롤백된다 — 그러면 포인트는
			// 이미 빠진 채로 요청만 PENDING 에 갇힌다. 그래서 삼키고 환급까지 같은 트랜잭션에서 끝낸다.
			log.warn("환전 승인 중 송금 실패. 환급 처리한다. cashoutRequestId={}, childId={}, pointAmount={}, cause={}",
					cashoutRequestId, childId, request.getPointAmount(), e.getMessage());
			pointDeductionService.refund(childId, request.getPointAmount(), REFUND_DESCRIPTION,
					idempotencyKey == null ? null : idempotencyKey + ":refund");
			request.fail();
		}

		return PointCashoutRequestResult.from(request);
	}

	/**
	 * 자녀가 자기 PENDING 요청을 직접 취소한다. 취소는 요청을 만든 자녀만 할 수 있다.
	 * 차감했던 포인트는 즉시 환급한다.
	 *
	 * @throws BusinessException FORBIDDEN 본인의 요청이 아닌 경우,
	 *         CASHOUT_REQUEST_NOT_FOUND 요청이 없거나 경로와 다른 경우,
	 *         CASHOUT_REQUEST_ALREADY_PROCESSED 이미 처리된 요청인 경우
	 */
	@Transactional
	public PointCashoutRequestResult cancel(Integer cashoutRequestId, Integer familyId, Integer childId,
			Integer loginChildId, String idempotencyKey) {

		if (!childId.equals(loginChildId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
					"본인의 환전 요청만 취소할 수 있습니다. loginUserId=" + loginChildId + ", childId=" + childId);
		}

		PointCashoutRequest request = lockPendingRequest(cashoutRequestId, familyId, childId);

		pointDeductionService.refund(childId, request.getPointAmount(), CANCEL_REFUND_DESCRIPTION,
				idempotencyKey == null ? null : idempotencyKey + ":cancel");
		request.cancel();

		log.info("환전 요청 취소. cashoutRequestId={}, familyId={}, childId={}, pointAmount={}",
				cashoutRequestId, familyId, childId, request.getPointAmount());
		return PointCashoutRequestResult.from(request);
	}

	/**
	 * 부모가 자녀의 PENDING 요청을 거절한다. 승인과 같은 권한 규칙이다 — 그 가족의
	 * 어느 부모나 거절할 수 있다. 차감했던 포인트는 즉시 환급한다.
	 *
	 * @throws BusinessException FORBIDDEN 해당 가족의 부모가 아닌 경우,
	 *         CASHOUT_REQUEST_NOT_FOUND 요청이 없거나 경로와 다른 경우,
	 *         CASHOUT_REQUEST_ALREADY_PROCESSED 이미 처리된 요청인 경우
	 */
	@Transactional
	public PointCashoutRequestResult decline(Integer cashoutRequestId, Integer familyId, Integer childId,
			Integer loginParentId, String idempotencyKey) {

		if (!familyMembershipReader.isParentOfFamily(familyId, loginParentId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN,
					"해당 가족의 부모가 아닙니다. familyId=" + familyId + ", parentId=" + loginParentId);
		}

		PointCashoutRequest request = lockPendingRequest(cashoutRequestId, familyId, childId);

		pointDeductionService.refund(childId, request.getPointAmount(), DECLINE_REFUND_DESCRIPTION,
				idempotencyKey == null ? null : idempotencyKey + ":decline");
		request.decline();

		log.info("환전 요청 거절. cashoutRequestId={}, familyId={}, childId={}, parentId={}, pointAmount={}",
				cashoutRequestId, familyId, childId, loginParentId, request.getPointAmount());
		return PointCashoutRequestResult.from(request);
	}

	/**
	 * 요청이 그 경로의 것인지 확인하고, 승인·취소·거절 전에 행을
	 * {@code SELECT ... FOR UPDATE} 로 잠근다. 아직 PENDING 인지도 여기서 확인한다.
	 */
	private PointCashoutRequest lockPendingRequest(Integer cashoutRequestId, Integer familyId, Integer childId) {
		PointCashoutRequest request = pointCashoutRequestRepository.findByCashoutRequestIdForUpdate(cashoutRequestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CASHOUT_REQUEST_NOT_FOUND,
						"환전 요청을 찾을 수 없습니다. cashoutRequestId=" + cashoutRequestId));

		if (!request.getFamilyId().equals(familyId) || !request.getChildId().equals(childId)) {
			throw new BusinessException(ErrorCode.CASHOUT_REQUEST_NOT_FOUND,
					"경로와 일치하지 않는 환전 요청입니다. cashoutRequestId=" + cashoutRequestId
							+ ", familyId=" + familyId + ", childId=" + childId);
		}

		if (request.getStatus() != PointCashoutRequestStatus.PENDING) {
			throw new BusinessException(ErrorCode.CASHOUT_REQUEST_ALREADY_PROCESSED,
					"이미 처리된 환전 요청입니다. cashoutRequestId=" + cashoutRequestId + ", status=" + request.getStatus());
		}

		return request;
	}

	/**
	 * 자녀 본인 또는 그 가족 부모가 환전 요청 목록을 본다. 조회 권한 판정은 잔액·거래내역과
	 * 같은 규칙이라 {@link PointWalletQueryService#validateReadAccess} 를 그대로 쓴다.
	 */
	@Transactional(readOnly = true)
	public List<PointCashoutRequestResult> list(Integer familyId, Integer childId, Integer loginUserId,
			PointCashoutRequestStatus status) {

		pointWalletQueryService.validateReadAccess(familyId, childId, loginUserId);

		List<PointCashoutRequest> requests = (status == null)
				? pointCashoutRequestRepository.findByFamilyIdAndChildIdOrderByRequestedAtDesc(familyId, childId)
				: pointCashoutRequestRepository.findByFamilyIdAndChildIdAndStatusOrderByRequestedAtDesc(
						familyId, childId, status);

		return requests.stream().map(PointCashoutRequestResult::from).toList();
	}
}
