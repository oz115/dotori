package com.bana.dotori.wallet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.mock.bank.BankTransferClient;
import com.bana.dotori.mock.bank.BankTransferResult;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.wallet.repository.FamilyMembershipReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WLT-01 포인트 충전.
 *
 * <p>아이가 실제 금액을 부모 계좌로 보내고, 팀 환전 정책에 따라
 * <b>현금 10원당 포인트 1점</b>을 돌려받는다. 예를 들어 10,000원을 보내면 1,000포인트가 발행된다.
 *
 * <p><b>10원 단위만 받는다.</b> 나누어떨어지지 않는 금액을 허용하면 버림으로 잃어버린 잔돈이
 * 아이 계좌에서는 빠졌는데 포인트로는 돌아오지 않는다. 반올림·이월 장부를 따로 두는 대신
 * 애초에 거부하는 쪽이 원장을 단순하게 유지한다.
 *
 * <p><b>순서.</b> 포인트 발행을 먼저 하고 송금을 나중에 한다.
 * 금액·잔액 상한·멱등성 검증을 모두 통과한 뒤에야 실제 돈을 움직이기 위해서다.
 * 송금을 먼저 하면, 뒤이은 포인트 발행이 검증에 걸려 실패했을 때
 * 돈만 옮겨지고 포인트는 없는 상태가 된다.
 * 명세의 처리 흐름은 "① 송금 ② 포인트 발행"으로 적혀 있지만 그것은 업무 흐름 설명이고,
 * 두 작업이 한 트랜잭션에서 함께 성공·실패하므로 결과는 동일하다.
 *
 * <p><b>트랜잭션.</b> 송금 호출을 트랜잭션 안에 두는 것은 팀 결정이다.
 * 송금이 실패하면 트랜잭션이 통째로 롤백돼 "자동 송금 실패 시 포인트를 발행하지 않는다"가
 * 자연스럽게 지켜진다. 대신 원격 호출이 끝날 때까지 지갑 행 락을 잡고 있으므로
 * 같은 아이의 동시 충전은 순차 처리되고, 송금 API 가 느려지면 이 구간이 병목이 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointChargeService {

	/** 팀 환전 정책. 현금 10원 = 포인트 1점. */
	public static final int KRW_PER_POINT = 10;

	private final PointWalletService pointWalletService;
	private final BankTransferClient bankTransferClient;
	private final FamilyMembershipReader familyMembershipReader;

	/**
	 * @param cashAmount 아이 계좌에서 빠져나갈 실제 금액(원). 10원 단위여야 한다
	 */
	@Transactional
	public PointChangeResult charge(Integer familyId, Integer childId, int cashAmount, String idempotencyKey) {
		// 환전 비율부터 본다. DB 나 은행을 건드리기 전에 걸러낼 수 있는 검증이다.
		int points = toPoints(cashAmount);

		// familyId 는 돈을 받을 부모 계좌를 결정한다. 소속을 확인하지 않으면 아이 계좌에서
		// 남의 가족 부모 계좌로 송금될 수 있으므로, 잔액을 건드리기 전에 먼저 막는다.
		if (!familyMembershipReader.isChildOfFamily(familyId, childId)) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD,
					"해당 가족의 자녀가 아닙니다. familyId=" + familyId + ", childId=" + childId);
		}

		// 첫 충전이면 지갑이 아직 없다. 가족 소속을 확인한 뒤에 만들어야
		// 아무 userId 나 넣어 지갑을 양산하는 것을 막을 수 있다.
		pointWalletService.createWalletIfAbsent(childId);

		PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(
				childId, points, PointTransactionReason.CHARGE, "포인트 충전", idempotencyKey));

		// 이미 처리된 요청이면 송금을 다시 보내면 안 된다. 돈이 두 번 빠져나간다.
		if (result.replayed()) {
			log.info("이미 처리된 충전 요청이라 송금을 생략한다. childId={}, idempotencyKey={}",
					childId, idempotencyKey);
			return result;
		}

		// 송금은 현금 금액으로 한다. 발행한 포인트가 아니라 아이가 실제로 보낸 돈이다.
		BankTransferResult transfer = bankTransferClient.transferFromChildToParent(familyId, childId, cashAmount,
				idempotencyKey);
		if (transfer.transferredAmount() != cashAmount) {
			// 부분 송금은 발행한 포인트와 금액이 어긋나므로 성공으로 볼 수 없다. 롤백시킨다.
			throw new BusinessException(ErrorCode.BANK_TRANSFER_FAILED,
					"요청 금액과 이체 금액이 다릅니다. childId=" + childId + ", 요청=" + cashAmount
							+ ", 이체=" + transfer.transferredAmount());
		}

		log.info("포인트 충전 완료. childId={}, cashAmount={}, points={}, balanceAfter={}, externalTransactionId={}",
				childId, cashAmount, points, result.balanceAfter(), transfer.externalTransactionId());
		return result;
	}

	/**
	 * 현금을 포인트로 환산한다.
	 *
	 * @throws BusinessException INVALID_CHARGE_UNIT 10원 단위가 아닌 경우
	 */
	private int toPoints(int cashAmount) {
		if (cashAmount % KRW_PER_POINT != 0) {
			throw new BusinessException(ErrorCode.INVALID_CHARGE_UNIT,
					"충전 금액은 " + KRW_PER_POINT + "원 단위여야 합니다. 요청 금액=" + cashAmount);
		}
		return cashAmount / KRW_PER_POINT;
	}
}
