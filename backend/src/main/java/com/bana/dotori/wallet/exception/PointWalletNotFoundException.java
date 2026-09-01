package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.BusinessException;

/**
 * 대상 사용자의 지갑이 아직 만들어지지 않은 경우.
 */
public class PointWalletNotFoundException extends BusinessException {

	public PointWalletNotFoundException(Integer userId) {
		super(WalletErrorCode.WALLET_NOT_FOUND,
				"포인트 지갑을 찾을 수 없습니다. userId=" + userId);
	}
}
