package com.bana.dotori.wallet.exception;

import com.bana.dotori.common.exception.BusinessException;

/**
 * 잔액이 컬럼 상한(INTEGER)을 넘어서는 경우.
 *
 * <p>{@code point_wallets.balance} 가 INTEGER 라 2,147,483,647 이 상한이다.
 * 이 검증이 없으면 오버플로로 잔액이 음수가 되거나 DB 에서 그대로 실패한다.
 */
public class PointBalanceExceededException extends BusinessException {

	public PointBalanceExceededException(int balance, int amount) {
		super(WalletErrorCode.POINT_BALANCE_EXCEEDED,
				"보유 가능한 최대 포인트를 초과했습니다. 잔액=" + balance + ", 지급 요청=" + amount
						+ ", 상한=" + Integer.MAX_VALUE);
	}
}
