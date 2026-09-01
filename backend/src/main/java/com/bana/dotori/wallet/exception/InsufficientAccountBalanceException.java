package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.BusinessException;

/**
 * 아이의 실제 계좌 잔액이 충전 금액보다 적은 경우.
 *
 * <p>포인트 잔액 부족({@link InsufficientPointException})과 헷갈리면 안 된다.
 * 이쪽은 은행 계좌의 원(￦) 잔액이고, API 명세(WLT-01)의 400 INSUFFICIENT_ACCOUNT_BALANCE 다.
 */
public class InsufficientAccountBalanceException extends BusinessException {

	public InsufficientAccountBalanceException(Integer childId, int accountBalance, int amount) {
		super(WalletErrorCode.INSUFFICIENT_ACCOUNT_BALANCE,
				"계좌 잔액이 부족합니다. childId=" + childId + ", 계좌 잔액=" + accountBalance + ", 송금 요청=" + amount);
	}
}
