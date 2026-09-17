package com.bana.dotori.savings.dto.response;

import java.math.BigDecimal;

import com.bana.dotori.savings.entity.ContributionStyle;
import com.bana.dotori.savings.entity.PayoutTiming;
import com.bana.dotori.savings.entity.SavingsProductType;

/**
 * GET /api/v1/families/{familyId}/children/{childId}/savings/products 응답 한 건.
 * {@code maxAmount} 는 이 가족의 weeklyRewardLimit 로 계산한, 지금 가입 가능한 최대 금액(X)이다.
 */
public record SavingsProductResponse(
	Integer savingsProductId,
	String name,
	SavingsProductType productType,
	String description,
	int interestRate,
	BigDecimal rewardRatio,
	ContributionStyle contributionStyle,
	PayoutTiming payoutTiming,
	Integer termWeeks,
	Integer installmentCount,
	String educationGoal,
	String preSignupMessage,
	long maxAmount
) {
}
