package com.bana.dotori.wallet.service;

import org.springframework.stereotype.Component;

import com.bana.dotori.user.service.RemainingAssetReader;
import com.bana.dotori.wallet.repository.PointWalletRepository;

import lombok.RequiredArgsConstructor;

/**
 * 잔여 자산 판정 중 포인트 부분 (MYPAGE-05).
 *
 * <p>지갑이 없으면 가진 포인트도 없으므로 잔여 자산이 아니다.
 */
@Component
@RequiredArgsConstructor
public class PointRemainingAssetReader implements RemainingAssetReader {

	private final PointWalletRepository pointWalletRepository;

	@Override
	public boolean hasRemainingAsset(Integer userId) {
		return pointWalletRepository.findByUserId(userId)
			.map(wallet -> wallet.getBalance() > 0)
			.orElse(false);
	}
}
