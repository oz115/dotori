package com.bana.dotori.wallet.exception;

import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.error.BusinessException;

/**
 * 지급 대상이 해당 가족의 자녀가 아닌 경우.
 *
 * <p>API 명세(WLT-02)의 403 NOT_FAMILY_CHILD 에 대응한다.
 * 다른 가족 아이에게 포인트를 발행해 버리는 사고를 막는 마지막 방어선이다.
 */
public class NotFamilyChildException extends BusinessException {

	public NotFamilyChildException(Integer familyId, Integer childId) {
		super(ErrorCode.NOT_FAMILY_CHILD,
				"해당 가족의 자녀가 아닙니다. familyId=" + familyId + ", childId=" + childId);
	}
}
