package com.bana.dotori.savings.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.family.dto.FamilySettings;
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
}
