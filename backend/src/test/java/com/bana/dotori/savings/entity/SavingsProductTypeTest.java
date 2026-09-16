package com.bana.dotori.savings.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.family.entity.FinancialKnowledgeLevel;

/**
 * 6개 예적금 상품의 X(최대 납입/예치 한도) 계산이, 상품마다 다른 이자 계산 방식을 거쳐도
 * 결국 같은 목표 총이자(REWARD_COEFFICIENT × rewardRatio × RL)에 도달하는지 검증한다.
 *
 * <p>이자는 {@code SavingsProductType.weeklyInterest} 를 실제로 주마다 돌려 쌓는다. 정산이
 * 하는 일과 같은 계산을 같은 메서드로 하므로, 여기서 맞으면 정산 결과도 맞는다.
 *
 * <p>복리예금만 예외다 — 원안 스펙이 의도적으로 목표의 4배를 지급하도록 잡아 뒀다.
 */
@DisplayName("SavingsProductType 최대 납입 한도와 만기 총이자")
class SavingsProductTypeTest {

	@Nested
	@DisplayName("보통예금")
	class Ordinary {

		@ParameterizedTest
		@ValueSource(longs = {1_000, 12_345, 100_000})
		@DisplayName("한 주 이자가 목표 총이자와 일치한다")
		void 한주_이자가_목표와_일치한다(long rewardLimit) {
			long maxAmount = SavingsProductType.ORDINARY.calculateMaxAmount(familySettingsOf(rewardLimit));
			long weeklyInterest = SavingsProductType.ORDINARY.weeklyInterest(maxAmount, 0);

			assertRoughly(weeklyInterest, SavingsProductType.ORDINARY.targetTotalInterest(rewardLimit));
		}
	}

	@Nested
	@DisplayName("단리예금")
	class SimpleInterest {

		@ParameterizedTest
		@ValueSource(longs = {1_000, 12_345, 100_000})
		@DisplayName("4주간 쌓은 이자 합계가 목표와 일치한다")
		void 만기_총이자가_목표와_일치한다(long rewardLimit) {
			long maxAmount = SavingsProductType.SIMPLE_INTEREST.calculateMaxAmount(familySettingsOf(rewardLimit));
			long totalInterest = accrueToMaturity(SavingsProductType.SIMPLE_INTEREST, maxAmount, 1);

			assertRoughly(totalInterest, SavingsProductType.SIMPLE_INTEREST.targetTotalInterest(rewardLimit));
		}

		@Test
		@DisplayName("원금이 그대로라 매주 같은 금액이 쌓인다")
		void 매주_같은_이자가_쌓인다() {
			long weekly = SavingsProductType.SIMPLE_INTEREST.weeklyInterest(10_000, 0);

			assertThat(weekly).isEqualTo(375);
			// 이미 쌓인 이자는 다음 이자의 기준에 들어가지 않는다 — 그것이 단리다.
			assertThat(SavingsProductType.SIMPLE_INTEREST.weeklyInterest(10_000, 375)).isEqualTo(375);
		}
	}

	@Nested
	@DisplayName("복리예금 (원안 스펙 그대로 — 같은 rewardRatio 의 단리 대비 4배 지급하는 예외)")
	class CompoundInterest {

		@ParameterizedTest
		@ValueSource(longs = {1_000, 12_345, 100_000})
		@DisplayName("만기 총이자가 목표(targetTotalInterest)의 4배다")
		void 만기_총이자가_목표의_4배다(long rewardLimit) {
			long maxAmount = SavingsProductType.COMPOUND_INTEREST.calculateMaxAmount(familySettingsOf(rewardLimit));
			long totalInterest = accrueToMaturity(SavingsProductType.COMPOUND_INTEREST, maxAmount, 1);
			long expected = SavingsProductType.COMPOUND_INTEREST.targetTotalInterest(rewardLimit) * 4;

			// 주마다 반올림하므로 4주치가 쌓이는 동안 1~2p 어긋날 수 있다.
			assertThat(totalInterest).isBetween(expected - 2, expected + 2);
		}

		@Test
		@DisplayName("쌓인 이자가 다음 주 이자의 기준에 들어간다")
		void 이자에_이자가_붙는다() {
			assertThat(SavingsProductType.COMPOUND_INTEREST.weeklyInterest(10_000, 0)).isEqualTo(1_000);
			assertThat(SavingsProductType.COMPOUND_INTEREST.weeklyInterest(10_000, 1_000)).isEqualTo(1_100);
		}
	}

	@Nested
	@DisplayName("정액 적립식 (소액·장기적금)")
	class FixedInstallmentProducts {

		@ParameterizedTest
		@ValueSource(longs = {1_000, 12_345, 100_000})
		@DisplayName("소액적금 — 2회 적립 동안 쌓은 이자가 목표와 일치한다")
		void 소액적금_총이자가_목표와_일치한다(long rewardLimit) {
			SavingsProductType type = SavingsProductType.SMALL_INSTALLMENT;
			long weekly = type.calculateMaxAmount(familySettingsOf(rewardLimit));

			assertRoughly(accrueToMaturity(type, weekly, type.getInstallmentCount()),
				type.targetTotalInterest(rewardLimit));
		}

		@ParameterizedTest
		@ValueSource(longs = {7_000, 12_345, 100_000})
		@DisplayName("장기적금 — 6회 적립 동안 쌓은 이자가 목표와 일치한다")
		void 장기적금_총이자가_목표와_일치한다(long rewardLimit) {
			SavingsProductType type = SavingsProductType.LONG_TERM_INSTALLMENT;
			long weekly = type.calculateMaxAmount(familySettingsOf(rewardLimit));

			assertRoughly(accrueToMaturity(type, weekly, type.getInstallmentCount()),
				type.targetTotalInterest(rewardLimit));
		}

		@Test
		@DisplayName("한도는 원안 그대로 소액적금 RL의 4/3배, 장기적금 RL의 2/7배다")
		void 한도는_원안_그대로다() {
			assertThat(SavingsProductType.SMALL_INSTALLMENT.calculateMaxAmount(familySettingsOf(3_000)))
				.isEqualTo(4_000);
			assertThat(SavingsProductType.LONG_TERM_INSTALLMENT.calculateMaxAmount(familySettingsOf(7_000)))
				.isEqualTo(2_000);
		}
	}

	@Nested
	@DisplayName("목표적금")
	class GoalInstallment {

		@ParameterizedTest
		@ValueSource(longs = {1_000, 12_345, 100_000})
		@DisplayName("목표를 달성하면 받는 이자가 목표와 일치한다")
		void 달성_이자가_목표와_일치한다(long rewardLimit) {
			SavingsProductType type = SavingsProductType.GOAL_INSTALLMENT;
			long goalAmount = type.calculateMaxAmount(familySettingsOf(rewardLimit));

			assertRoughly(type.goalAchievementInterest(goalAmount), type.targetTotalInterest(rewardLimit));
		}

		@Test
		@DisplayName("주간 정산으로는 이자가 붙지 않는다")
		void 주간_이자는_없다() {
			assertThat(SavingsProductType.GOAL_INSTALLMENT.weeklyInterest(10_000, 0)).isZero();
		}

		@Test
		@DisplayName("다른 상품은 목표 달성 이자를 주지 않는다")
		void 다른_상품은_달성_이자가_없다() {
			assertThat(SavingsProductType.SIMPLE_INTEREST.goalAchievementInterest(10_000)).isZero();
			assertThat(SavingsProductType.ORDINARY.goalAchievementInterest(10_000)).isZero();
		}
	}

	/**
	 * 주마다 반올림한 이자를 더하므로 목표와 1p 안팎으로 어긋날 수 있다. 그 정도는 같은 값으로 본다.
	 */
	private static void assertRoughly(long actual, long expected) {
		assertThat(actual).isBetween(expected - 1, expected + 1);
	}

	/**
	 * 만기까지 주마다 정산을 돌려 쌓인 이자 합계를 구한다. 정산이 하는 것과 같은 순서다 —
	 * 그 주의 이자를 먼저 쌓고, 남은 회차가 있으면 다음 주 납입으로 원금을 늘린다.
	 *
	 * @param weeklyAmount     거치식은 최초 원금, 정액 적립식은 회당 납입액
	 * @param installmentCount 총 납입 회차 (거치식은 1)
	 */
	private static long accrueToMaturity(SavingsProductType type, long weeklyAmount, int installmentCount) {
		long balance = weeklyAmount;
		long accrued = 0;
		for (int week = 1; week <= type.getTermWeeks(); week++) {
			accrued += type.weeklyInterest(balance, accrued);
			if (week < installmentCount) {
				balance += weeklyAmount;
			}
		}
		return accrued;
	}

	private static FamilySettings familySettingsOf(long rewardLimit) {
		return new FamilySettings(0, Math.toIntExact(rewardLimit), FinancialKnowledgeLevel.BEGINNER);
	}
}
