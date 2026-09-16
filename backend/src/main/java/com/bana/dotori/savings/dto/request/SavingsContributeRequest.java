package com.bana.dotori.savings.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 예적금 추가 납입 요청. 금액 규칙은 상품마다 다르다 — 정액 적립식(소액·장기적금)은 가입 때 정한
 * 회당 약정액과 정확히 같아야 하고, 보통예금·목표적금은 한도(잔액 상한 / 목표 금액) 안에서 자유다.
 */
public record SavingsContributeRequest(
	@NotNull(message = "납입 금액은 필수입니다")
	@Positive(message = "납입 금액은 1 이상이어야 합니다") Integer amount
) {
}
