package com.bana.dotori.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * WLT-01 충전 요청 본문. API 명세와 동일하게 금액 하나만 받는다.
 *
 * <pre>
 * { "amount": 10000 }   // 10,000원을 보내고 1,000포인트를 받는다
 * </pre>
 *
 * <p>여기 담기는 값은 <b>현금(원)</b>이다. 발행될 포인트가 아니다.
 * 환전 비율(10원 = 1포인트)과 10원 단위 검증은 정책이라
 * {@link com.bana.dotori.wallet.service.PointChargeService} 가 갖고 있다.
 * DTO 는 "0 이하가 아닌 정수인가"까지만 본다.
 *
 * @param amount 아이 계좌에서 빠져나갈 실제 금액(원). 10원 단위여야 한다
 */
public record PointChargeRequest(
		@NotNull(message = "충전 금액은 필수입니다")
		@Positive(message = "충전 금액은 1 이상이어야 합니다")
		Integer amount) {
}
