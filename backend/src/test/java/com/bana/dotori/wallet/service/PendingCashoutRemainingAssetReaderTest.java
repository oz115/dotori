package com.bana.dotori.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.wallet.entity.PointCashoutRequestStatus;
import com.bana.dotori.wallet.repository.PointCashoutRequestRepository;

@ExtendWith(MockitoExtension.class)
class PendingCashoutRemainingAssetReaderTest {

	@Mock
	private PointCashoutRequestRepository pointCashoutRequestRepository;

	@InjectMocks
	private PendingCashoutRemainingAssetReader reader;

	@Test
	@DisplayName("처리되지 않은 환전 요청이 있으면 자산이 남은 것으로 본다")
	void hasRemainingAsset_pendingExists() {
		given(pointCashoutRequestRepository.existsByChildIdAndStatusIn(2,
			List.of(PointCashoutRequestStatus.PENDING, PointCashoutRequestStatus.PROGRESS))).willReturn(true);

		assertThat(reader.hasRemainingAsset(2)).isTrue();
	}

	@Test
	@DisplayName("처리되지 않은 환전 요청이 없으면 자산이 남지 않은 것으로 본다")
	void hasRemainingAsset_noPending() {
		given(pointCashoutRequestRepository.existsByChildIdAndStatusIn(anyInt(), anyCollection()))
			.willReturn(false);

		assertThat(reader.hasRemainingAsset(2)).isFalse();
	}
}
