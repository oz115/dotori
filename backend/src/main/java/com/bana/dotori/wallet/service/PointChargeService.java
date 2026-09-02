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
 * <p>아이가 실제 금액을 부모 계좌로 보내고 같은 금액(1:1)을 포인트로 돌려받는다.
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

	private final PointWalletService pointWalletService;
	private final BankTransferClient bankTransferClient;
	private final FamilyMembershipReader familyMembershipReader;

	@Transactional
	public PointChangeResult charge(Integer familyId, Integer childId, int amount, String idempotencyKey) {
		// familyId 는 돈을 받을 부모 계좌를 결정한다. 소속을 확인하지 않으면 아이 계좌에서
		// 남의 가족 부모 계좌로 송금될 수 있으므로, 잔액을 건드리기 전에 먼저 막는다.
		if (!familyMembershipReader.isChildOfFamily(familyId, childId)) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_CHILD,
					"해당 가족의 자녀가 아닙니다. familyId=" + familyId + ", childId=" + childId);
		}

		PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(
				childId, amount, PointTransactionReason.CHARGE, "포인트 충전", idempotencyKey));

		// 이미 처리된 요청이면 송금을 다시 보내면 안 된다. 돈이 두 번 빠져나간다.
		if (result.replayed()) {
			log.info("이미 처리된 충전 요청이라 송금을 생략한다. childId={}, idempotencyKey={}",
					childId, idempotencyKey);
			return result;
		}

		BankTransferResult transfer = bankTransferClient.transferFromChildToParent(familyId, childId, amount);
		if (transfer.transferredAmount() != amount) {
			// 부분 송금은 발행한 포인트와 금액이 어긋나므로 성공으로 볼 수 없다. 롤백시킨다.
			throw new BusinessException(ErrorCode.BANK_TRANSFER_FAILED,
					"요청 금액과 이체 금액이 다릅니다. childId=" + childId + ", 요청=" + amount
							+ ", 이체=" + transfer.transferredAmount());
		}

		log.info("포인트 충전 완료. childId={}, amount={}, balanceAfter={}, externalTransactionId={}",
				childId, amount, result.balanceAfter(), transfer.externalTransactionId());
		return result;
	}
}
