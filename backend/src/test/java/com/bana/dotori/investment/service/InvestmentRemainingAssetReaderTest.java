package com.bana.dotori.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.investment.repository.InvestmentHoldingRepository;

@ExtendWith(MockitoExtension.class)
class InvestmentRemainingAssetReaderTest {

	@Mock
	private InvestmentHoldingRepository investmentHoldingRepository;

	@InjectMocks
	private InvestmentRemainingAssetReader reader;

	@Test
	@DisplayName("보유 중인 종목이 있으면 자산이 남은 것으로 본다")
	void hasRemainingAsset_holding() {
		given(investmentHoldingRepository.existsByChildId(2)).willReturn(true);

		assertThat(reader.hasRemainingAsset(2)).isTrue();
	}

	@Test
	@DisplayName("보유 종목이 없으면 자산이 남지 않은 것으로 본다")
	void hasRemainingAsset_none() {
		given(investmentHoldingRepository.existsByChildId(2)).willReturn(false);

		assertThat(reader.hasRemainingAsset(2)).isFalse();
	}
}
