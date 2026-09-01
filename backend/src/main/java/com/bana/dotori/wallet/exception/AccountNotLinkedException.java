package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;

/**
 * 충전에 필요한 연동 계좌가 없는 경우. 아이 쪽일 수도 부모 쪽일 수도 있다.
 *
 * <p>API 명세(WLT-01)의 404 ACCOUNT_NOT_LINKED 에 대응한다.
 * 계좌 도메인(ACC-01·02)이 만들어지면 그쪽으로 옮기는 것이 맞다.
 */
public class AccountNotLinkedException extends BusinessException {

	public AccountNotLinkedException(String owner, Integer userId) {
		super(ErrorCode.ACCOUNT_NOT_LINKED,
				"연동된 계좌가 없습니다. owner=" + owner + ", userId=" + userId);
	}
}
