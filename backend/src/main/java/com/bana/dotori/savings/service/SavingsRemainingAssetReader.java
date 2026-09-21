package com.bana.dotori.savings.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.repository.SavingsSubscriptionRepository;
import com.bana.dotori.user.service.RemainingAssetReader;

import lombok.RequiredArgsConstructor;

/**
 * 잔여 자산 판정 중 예적금 부분.
 *
 * <p>진행 중(ACTIVE)이거나 만기가 됐지만 아직 수령하지 않은(MATURED) 가입 건은 원금과 이자가
 * 지갑 밖에 묶여 있는 자산이다. 해지(CANCELLED)·수령(COMPLETED)으로 끝난 건은 지갑으로 빠져나가
 * 더는 자산이 아니다.
 */
@Component
@RequiredArgsConstructor
public class SavingsRemainingAssetReader implements RemainingAssetReader {

	private static final List<SavingsSubscriptionStatus> HOLDING = List.of(
		SavingsSubscriptionStatus.ACTIVE, SavingsSubscriptionStatus.MATURED);

	private final SavingsSubscriptionRepository savingsSubscriptionRepository;

	@Override
	public boolean hasRemainingAsset(Integer userId) {
		return savingsSubscriptionRepository.existsByChildIdAndStatusIn(userId, HOLDING);
	}
}
