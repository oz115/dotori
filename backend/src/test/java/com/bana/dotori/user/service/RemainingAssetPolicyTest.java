package com.bana.dotori.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RemainingAssetPolicyTest {

	private static RemainingAssetReader reader(boolean hasAsset) {
		return userId -> hasAsset;
	}

	@Test
	@DisplayName("모든 도메인에 자산이 없으면 남은 자산이 없다")
	void hasRemainingAsset_noneLeft() {
		RemainingAssetPolicy policy = new RemainingAssetPolicy(List.of(reader(false), reader(false)));

		assertThat(policy.hasRemainingAsset(1)).isFalse();
	}

	@Test
	@DisplayName("어느 한 도메인이라도 자산이 남아 있으면 남은 자산이 있다")
	void hasRemainingAsset_anyDomain() {
		RemainingAssetPolicy policy = new RemainingAssetPolicy(
			List.of(reader(false), reader(true), reader(false)));

		assertThat(policy.hasRemainingAsset(1)).isTrue();
	}

	@Test
	@DisplayName("판정 도메인이 하나도 없으면 남은 자산이 없다")
	void hasRemainingAsset_noReaders() {
		RemainingAssetPolicy policy = new RemainingAssetPolicy(List.of());

		assertThat(policy.hasRemainingAsset(1)).isFalse();
	}
}
