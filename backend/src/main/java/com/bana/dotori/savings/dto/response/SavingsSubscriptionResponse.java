package com.bana.dotori.savings.dto.response;

import java.time.OffsetDateTime;

import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;

/**
 * 예적금 가입 건 하나 (가입 응답이자 목록·상세 조회 응답으로 함께 쓴다).
 *
 * @param currentAmount    지금까지 납입된 원금 누계 (이자 제외)
 * @param accruedInterest  지금까지 쌓인 이자. 보통예금은 매주 지갑으로 바로 나가므로 늘 0이다
 * @param totalAmount      원금 + 이자. 지금 만기가 오면 받게 될 금액이다
 * @param receivedAt       수령(또는 해지 반환)이 끝난 시각. 아직이면 null
 */
public record SavingsSubscriptionResponse(
	Integer savingsSubscriptionId,
	Integer savingsProductId,
	String productName,
	SavingsProductType productType,
	int principal,
	int currentAmount,
	int accruedInterest,
	int totalAmount,
	int contributionCount,
	SavingsSubscriptionStatus status,
	OffsetDateTime startedAt,
	OffsetDateTime maturityAt,
	OffsetDateTime nextSettlementAt,
	OffsetDateTime receivedAt
) {
}
