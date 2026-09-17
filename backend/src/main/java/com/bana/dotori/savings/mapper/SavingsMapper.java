package com.bana.dotori.savings.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.savings.dto.response.SavingsExpectedReturnResponse;
import com.bana.dotori.savings.dto.response.SavingsProductResponse;
import com.bana.dotori.savings.dto.response.SavingsSubscriptionResponse;
import com.bana.dotori.savings.dto.response.SavingsSummaryResponse;
import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;

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
			product.getContributionStyle(),
			product.getInstallmentCount(),
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

	/**
	 * 부모 홈 카드용 요약. 넘어온 가입 건은 이미 진행 중·수령 대기로 걸러져 있다고 본다 —
	 * 어느 상태를 "아이의 예적금 잔액"으로 볼지 정하는 것은 서비스의 일이다.
	 */
	public SavingsSummaryResponse toSummaryResponse(List<SavingsSubscription> subscriptions) {
		int activeCount = 0;
		int maturedCount = 0;
		int totalCurrentAmount = 0;
		int totalAccruedInterest = 0;
		OffsetDateTime nearestMaturityAt = null;

		for (SavingsSubscription subscription : subscriptions) {
			if (subscription.getStatus() == SavingsSubscriptionStatus.ACTIVE) {
				activeCount++;
			} else if (subscription.getStatus() == SavingsSubscriptionStatus.MATURED) {
				maturedCount++;
			}
			totalCurrentAmount += subscription.getCurrentAmount();
			totalAccruedInterest += subscription.getAccruedInterest();
			nearestMaturityAt = earlierMaturity(nearestMaturityAt, subscription);
		}

		return new SavingsSummaryResponse(activeCount, maturedCount, totalCurrentAmount, totalAccruedInterest,
			totalCurrentAmount + totalAccruedInterest, nearestMaturityAt);
	}

	/**
	 * 만기가 더 이른 쪽을 고른다. 이미 만기에 닿아 수령을 기다리는 건은 "다가올 만기"가 아니라
	 * 세지 않는다 — 그쪽은 {@code maturedCount} 가 따로 알려 준다.
	 */
	private OffsetDateTime earlierMaturity(OffsetDateTime nearest, SavingsSubscription subscription) {
		OffsetDateTime maturityAt = subscription.getMaturityAt();
		if (!subscription.isActive() || maturityAt == null) {
			return nearest;
		}
		return (nearest == null || maturityAt.isBefore(nearest)) ? maturityAt : nearest;
	}
}
