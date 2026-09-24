package com.bana.dotori.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * WLT-07 환전 요청 본문.
 *
 * <pre>
 * { "amount": 1000 }   // 보유 포인트 1,000점을 현금으로 바꿔 달라는 요청
 * </pre>
 *
 * <p>여기 담기는 값은 <b>포인트</b>다. 승인 시 실제로 송금될 현금(원)이 아니다.
 * 환전 비율(포인트 1점 = 10원)은 정책이라 {@code PointCashoutRequestService}가 갖고 있다.
 * DTO 는 "0 이하가 아닌 정수인가"까지만 본다.
 *
 * @param amount 현금으로 바꿀 포인트 수량
 */
public record PointCashoutRequestCreateRequest(
		@NotNull(message = "환전 포인트는 필수입니다")
		@Positive(message = "환전 포인트는 1 이상이어야 합니다")
		Integer amount) {
}
