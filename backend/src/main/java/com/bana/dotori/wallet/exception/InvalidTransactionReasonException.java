package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;

import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * 거래 사유와 증감 방향이 맞지 않는 경우. 예를 들어 GRANT 사유로 차감을 요청한 경우다.
 *
 * <p>사용자가 만들 수 있는 상황이 아니라 호출하는 코드의 실수이므로 500 으로 둔다.
 * 사용자에게 보여 줄 에러가 아니라 개발 중에 잡아야 할 에러라는 뜻이다.
 */
public class InvalidTransactionReasonException extends BusinessException {

	public InvalidTransactionReasonException(PointTransactionReason reason, String direction) {
		super(ErrorCode.INVALID_TRANSACTION_REASON,
				"거래 사유와 방향이 맞지 않습니다. reason=" + reason + ", direction=" + direction);
	}
}
