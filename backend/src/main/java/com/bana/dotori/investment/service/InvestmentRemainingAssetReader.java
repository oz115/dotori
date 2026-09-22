package com.bana.dotori.investment.service;

import org.springframework.stereotype.Component;

import com.bana.dotori.investment.repository.InvestmentHoldingRepository;
import com.bana.dotori.user.service.RemainingAssetReader;

import lombok.RequiredArgsConstructor;

/**
 * 잔여 자산 판정 중 투자 부분. 보유 행이 남아 있으면(매도하지 않은 종목이 있으면) 자산이 있는 것이다.
 * 매도하면 보유 행이 삭제되고 대금이 지갑으로 들어간다.
 */
@Component
@RequiredArgsConstructor
public class InvestmentRemainingAssetReader implements RemainingAssetReader {

	private final InvestmentHoldingRepository investmentHoldingRepository;

	@Override
	public boolean hasRemainingAsset(Integer userId) {
		return investmentHoldingRepository.existsByChildId(userId);
	}
}
