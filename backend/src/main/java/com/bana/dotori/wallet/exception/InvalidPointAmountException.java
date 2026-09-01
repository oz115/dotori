package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.BusinessException;

/**
 * 0 이하의 금액으로 포인트 변경을 요청한 경우.
 */
public class InvalidPointAmountException extends BusinessException {

	public InvalidPointAmountException(int amount) {
		super(WalletErrorCode.INVALID_POINT_AMOUNT,
				"포인트 금액은 1 이상이어야 합니다. 요청 금액=" + amount);
	}
}
