package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;

/**
 * 아이 계좌에서 부모 계좌로 실제 송금하는 데 실패한 경우.
 *
 * <p>이 예외가 트랜잭션을 롤백시켜 포인트가 발행되지 않도록 만든다.
 * 스토리의 "자동 송금 실패 시 포인트를 발행하지 않는다" 조건이 이 경로로 지켜진다.
 */
public class BankTransferFailedException extends BusinessException {

	public BankTransferFailedException(Integer childId, int amount, String reason) {
		super(ErrorCode.BANK_TRANSFER_FAILED,
				"계좌 송금에 실패했습니다. childId=" + childId + ", amount=" + amount + ", reason=" + reason);
	}

	public BankTransferFailedException(Integer childId, int amount, Throwable cause) {
		super(ErrorCode.BANK_TRANSFER_FAILED,
				"계좌 송금에 실패했습니다. childId=" + childId + ", amount=" + amount, cause);
	}
}
