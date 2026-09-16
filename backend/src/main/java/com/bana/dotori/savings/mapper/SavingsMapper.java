package com.bana.dotori.savings.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.savings.dto.response.SavingsExpectedReturnResponse;
import com.bana.dotori.savings.dto.response.SavingsProductResponse;
import com.bana.dotori.savings.dto.response.SavingsSubscriptionResponse;
import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsSubscription;

@Component
public class SavingsMapper {

	public SavingsProductResponse toProductResponse(SavingsProduct product, FamilySettings familySettings) {
		long maxAmount = product.getProductType().calculateMaxAmount(familySettings);
		return new SavingsProductResponse(
			product.getId(),
			product.getName(),
			product.getProductType(),
			product.getDescription(),
			product.getInterestRate(),
			product.getRewardRatio(),
			product.getContributionStyle(),
			product.getPayoutTiming(),
			product.getTermWeeks(),
			product.getInstallmentCount(),
			product.getEducationGoal(),
			product.getPreSignupMessage(),
			maxAmount);
	}

	public SavingsSubscriptionResponse toSubscriptionResponse(SavingsSubscription subscription,
		SavingsProduct product) {
		return new SavingsSubscriptionResponse(
			subscription.getId(),
			product.getId(),
			product.getName(),
			product.getProductType(),
			subscription.getPrincipal(),
			subscription.getCurrentAmount(),
			subscription.getAccruedInterest(),
			subscription.payoutAmount(),
			subscription.getContributionCount(),
			subscription.getStatus(),
			subscription.getStartedAt(),
			subscription.getMaturityAt(),
			subscription.getNextSettlementAt(),
			subscription.getReceivedAt());
	}

	/**
	 * @param remainingInterest 앞으로 더 쌓일 이자 (이미 쌓인 것은 제외)
	 */
	public SavingsExpectedReturnResponse toExpectedReturnResponse(SavingsSubscription subscription,
		long remainingInterest, long earlyTerminationAmount) {
		long expectedInterest = subscription.getAccruedInterest() + remainingInterest;
		return new SavingsExpectedReturnResponse(
			subscription.getId(),
			subscription.getCurrentAmount(),
			subscription.getAccruedInterest(),
			expectedInterest,
			subscription.getCurrentAmount() + expectedInterest,
			earlyTerminationAmount,
			subscription.getMaturityAt());
	}
}
