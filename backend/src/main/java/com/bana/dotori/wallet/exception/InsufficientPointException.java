package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.BusinessException;

/**
 * 보유 포인트보다 많은 금액을 차감하려 한 경우.
 */
public class InsufficientPointException extends BusinessException {

	public InsufficientPointException(int balance, int amount) {
		super(WalletErrorCode.INSUFFICIENT_POINT,
				"보유 포인트가 부족합니다. 잔액=" + balance + ", 차감 요청=" + amount);
	}
}
