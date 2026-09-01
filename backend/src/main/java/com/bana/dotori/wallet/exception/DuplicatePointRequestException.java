package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;

/**
 * 같은 멱등키가 다른 지갑의 거래에 이미 사용된 경우.
 *
 * <p>같은 지갑에 대한 중복 요청은 예외가 아니라 기존 거래를 그대로 돌려주는 것이 정상 동작이다.
 * 이 예외는 클라이언트가 서로 다른 대상에 같은 키를 재사용한 잘못된 호출에만 발생한다.
 */
public class DuplicatePointRequestException extends BusinessException {

	public DuplicatePointRequestException(String idempotencyKey, Throwable cause) {
		super(ErrorCode.DUPLICATE_POINT_REQUEST,
				"이미 다른 거래에 사용된 요청 키입니다. idempotencyKey=" + idempotencyKey, cause);
	}
}
