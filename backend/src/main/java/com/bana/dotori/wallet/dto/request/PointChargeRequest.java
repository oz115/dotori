package com.bana.dotori.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * WLT-01 충전 요청 본문. API 명세와 동일하게 금액 하나만 받는다.
 *
 * <pre>
 * { "amount": 10000 }
 * </pre>
 *
 * @param amount 충전 금액(원). 원과 포인트는 1:1 이다
 */
public record PointChargeRequest(
		@NotNull(message = "충전 금액은 필수입니다")
		@Positive(message = "충전 금액은 1 이상이어야 합니다")
		Integer amount) {
}
