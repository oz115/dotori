package com.bana.dotori.savings.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 예적금 가입 요청. {@code amount} 의 의미는 상품마다 다르다 — 거치식(단리·복리)은 최초 납입 원금,
 * 정액 적립식(소액·장기)은 회당 납입액, 목표적금은 목표 금액이다.
 */
public record SavingsSubscribeRequest(
	@NotNull(message = "가입할 상품을 선택해야 합니다") Integer savingsProductId,
	@NotNull(message = "가입 금액은 필수입니다")
	@Positive(message = "가입 금액은 1 이상이어야 합니다") Integer amount
) {
}
