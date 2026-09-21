package com.bana.dotori.savings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.repository.SavingsSubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class SavingsRemainingAssetReaderTest {

	@Mock
	private SavingsSubscriptionRepository savingsSubscriptionRepository;

	@InjectMocks
	private SavingsRemainingAssetReader reader;

	@Test
	@DisplayName("진행 중이거나 수령 대기인 예적금이 있으면 자산이 남은 것으로 본다")
	void hasRemainingAsset_holding() {
		given(savingsSubscriptionRepository.existsByChildIdAndStatusIn(2,
			List.of(SavingsSubscriptionStatus.ACTIVE, SavingsSubscriptionStatus.MATURED))).willReturn(true);

		assertThat(reader.hasRemainingAsset(2)).isTrue();
	}

	@Test
	@DisplayName("해지·수령으로 끝난 예적금만 있으면 자산이 남지 않은 것으로 본다")
	void hasRemainingAsset_onlyFinished() {
		given(savingsSubscriptionRepository.existsByChildIdAndStatusIn(2,
			List.of(SavingsSubscriptionStatus.ACTIVE, SavingsSubscriptionStatus.MATURED))).willReturn(false);

		assertThat(reader.hasRemainingAsset(2)).isFalse();
	}
}
