package com.bana.dotori.savings.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.entity.SavingsSubscription;

/**
 * SAV-05 예상 손익 계산. 정산이 실제로 쌓을 이자와 같은 값이 나오는지가 핵심이라,
 * 기대값은 {@code SavingsProductTypeTest} 가 검산한 만기 총이자에서 가져온다.
 */
@DisplayName("SavingsExpectedReturnCalculator 만기 예상 이자")
class SavingsExpectedReturnCalculatorTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer PRODUCT_ID = 1;

	private final SavingsExpectedReturnCalculator calculator = new SavingsExpectedReturnCalculator();

	private SavingsProduct product(SavingsProductType type) {
		return SavingsProduct.builder().productType(type).name(type.name()).description("설명").build();
	}

	/**
	 * @param weeksElapsed 가입 후 지난 주수. 그만큼 정산이 끝났다고 보고 nextSettlementAt 을 잡는다
	 */
	private SavingsSubscription subscription(SavingsProduct product, int principal, int currentAmount,
		int contributionCount, int weeksElapsed) {
		OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(weeksElapsed);
		Integer termWeeks = product.getTermWeeks();
		SavingsSubscription subscription = SavingsSubscription.subscribe(FAMILY_ID, CHILD_ID, PRODUCT_ID, principal,
			currentAmount, contributionCount > 0, "key", startedAt,
			termWeeks == null ? null : startedAt.plusWeeks(termWeeks), startedAt.plusWeeks(weeksElapsed + 1L));
		ReflectionTestUtils.setField(subscription, "contributionCount", contributionCount);
		return subscription;
	}

	@Nested
	@DisplayName("거치식")
	class LumpSum {

		@Test
		@DisplayName("가입 직후 단리예금은 만기 총이자(원금의 15%)를 예상한다")
		void 단리예금_만기총이자() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);

			long interest = calculator.remainingInterest(subscription(simple, 10_000, 10_000, 0, 0), simple);

			assertThat(interest).isEqualTo(1_500);
		}

		@Test
		@DisplayName("절반 지난 단리예금은 남은 주치만 예상한다")
		void 단리예금_남은주치만() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			SavingsSubscription subscription = subscription(simple, 10_000, 10_000, 0, 2);
			subscription.accrueInterest(750);

			// 4주 중 2주가 지났으니 남은 이자는 두 주치 = 375 × 2.
			assertThat(calculator.remainingInterest(subscription, simple)).isEqualTo(750);
		}

		@Test
		@DisplayName("복리예금은 쌓인 이자까지 기준에 넣어 굴린다")
		void 복리예금은_이자에_이자가_붙는다() {
			SavingsProduct compound = product(SavingsProductType.COMPOUND_INTEREST);

			long interest = calculator.remainingInterest(subscription(compound, 10_000, 10_000, 0, 0), compound);

			// 10_000 × (1.1^4 - 1) = 4_641. 주마다 반올림하므로 1~2p 오차를 허용한다.
			assertThat(interest).isBetween(4_639L, 4_643L);
		}
	}

	@Nested
	@DisplayName("정액 적립식")
	class FixedInstallment {

		@Test
		@DisplayName("남은 회차를 약속대로 넣는다고 보고 계산한다")
		void 남은_회차를_넣는다고_본다() {
			SavingsProduct small = product(SavingsProductType.SMALL_INSTALLMENT);

			// 가입 직후: 1회차만 납입된 상태. 2회차를 마저 넣으면 만기 총이자는 0.15 × 회당 납입액.
			long interest = calculator.remainingInterest(subscription(small, 1_000, 1_000, 1, 0), small);

			assertThat(interest).isEqualTo(150);
		}

		@Test
		@DisplayName("장기적금도 6회 완납 기준으로 계산한다")
		void 장기적금_완납_기준() {
			SavingsProduct longTerm = product(SavingsProductType.LONG_TERM_INSTALLMENT);

			long interest = calculator.remainingInterest(subscription(longTerm, 1_000, 1_000, 1, 0), longTerm);

			// (1+2+…+6) × 1_000 ÷ 6 × 20% = 700.
			assertThat(interest).isEqualTo(700);
		}
	}

	@Nested
	@DisplayName("만기가 없거나 목표로 지급하는 상품")
	class SpecialCases {

		@Test
		@DisplayName("보통예금은 만기가 없어 다음 한 주치를 예상한다")
		void 보통예금은_한주치다() {
			SavingsProduct ordinary = product(SavingsProductType.ORDINARY);

			assertThat(calculator.remainingInterest(subscription(ordinary, 40_000, 40_000, 0, 0), ordinary))
				.isEqualTo(400);
		}

		@Test
		@DisplayName("목표적금은 목표를 채운다고 보고 목표 금액 기준 이자를 예상한다")
		void 목표적금은_목표_달성을_가정한다() {
			SavingsProduct goal = product(SavingsProductType.GOAL_INSTALLMENT);

			// 아직 3_000 밖에 못 모았어도 목표(10_000)를 채웠을 때의 이자를 보여준다.
			assertThat(calculator.remainingInterest(subscription(goal, 10_000, 3_000, 2, 1), goal))
				.isEqualTo(1_500);
		}

		@Test
		@DisplayName("이미 끝난 건은 더 쌓일 이자가 없다")
		void 끝난_건은_0이다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			SavingsSubscription subscription = subscription(simple, 10_000, 10_000, 0, 4);
			subscription.markMatured();

			assertThat(calculator.remainingInterest(subscription, simple)).isZero();
		}
	}

	@Test
	@DisplayName("지금 해지하면 이자 없이 납입 원금만 받는다")
	void 해지시에는_원금만_받는다() {
		SavingsProduct small = product(SavingsProductType.SMALL_INSTALLMENT);
		SavingsSubscription subscription = subscription(small, 1_000, 2_000, 2, 1);
		subscription.accrueInterest(100);

		assertThat(calculator.earlyTerminationAmount(subscription)).isEqualTo(2_000);
	}
}
