package com.bana.dotori.savings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.entity.SavingsTransactionType;
import com.bana.dotori.savings.repository.SavingsProductRepository;
import com.bana.dotori.savings.repository.SavingsSubscriptionRepository;
import com.bana.dotori.savings.repository.SavingsTransactionRepository;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointWalletService;

/**
 * 2계층 - 주간 정산. 이자를 얼마 쌓는지(상품별 계산식)는 {@code SavingsProductTypeTest} 가 검산하므로,
 * 여기서는 "쌓느냐 지급하느냐", "언제 만기로 바뀌느냐", "밀린 주를 따라잡느냐"만 본다.
 */
@DisplayName("SavingsSettlementService 주간 정산")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SavingsSettlementServiceTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer PRODUCT_ID = 1;
	private static final Integer SUBSCRIPTION_ID = 77;

	@Mock
	private SavingsSubscriptionRepository savingsSubscriptionRepository;

	@Mock
	private SavingsProductRepository savingsProductRepository;

	@Mock
	private SavingsTransactionRepository savingsTransactionRepository;

	@Mock
	private PointWalletService pointWalletService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private SavingsSettlementService savingsSettlementService;

	private SavingsProduct product(SavingsProductType type) {
		SavingsProduct product = SavingsProduct.builder()
			.productType(type)
			.name(type.name())
			.description("설명")
			.build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
		given(savingsProductRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
		return product;
	}

	/**
	 * 정산 대상 가입 건. {@code startedAt} 을 기준으로 만기와 첫 정산 시각을 실제 가입과 똑같이 잡는다.
	 */
	private SavingsSubscription subscription(SavingsProduct product, int amount, OffsetDateTime startedAt) {
		Integer termWeeks = product.getTermWeeks();
		SavingsSubscription subscription = SavingsSubscription.subscribe(FAMILY_ID, CHILD_ID, PRODUCT_ID, amount,
			product.getContributionStyle().initialDeposit(amount),
			product.getContributionStyle().countsInitialDepositAsInstallment(), "key", startedAt,
			termWeeks == null ? null : startedAt.plusWeeks(termWeeks), startedAt.plusWeeks(1));
		ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
		given(savingsSubscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).willReturn(Optional.of(subscription));
		return subscription;
	}

	private void walletAccepts() {
		given(pointWalletService.deposit(any())).willReturn(new PointChangeResult(1, CHILD_ID,
			PointTransactionReason.SAVINGS_WITHDRAWAL, 0, 0, OffsetDateTime.now(), false));
	}

	@Nested
	@DisplayName("이자 처리")
	class Interest {

		@Test
		@DisplayName("보통예금은 이자를 쌓지 않고 그 주에 바로 지갑으로 보낸다")
		void 보통예금은_즉시_지급한다() {
			SavingsProduct ordinary = product(SavingsProductType.ORDINARY);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(1).minusHours(1);
			SavingsSubscription subscription = subscription(ordinary, 40_000, startedAt);
			walletAccepts();

			int interest = savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			assertThat(interest).isEqualTo(400);
			assertThat(subscription.getAccruedInterest()).isZero();
			assertThat(subscription.getStatus()).isEqualTo(SavingsSubscriptionStatus.ACTIVE);
			verify(pointWalletService).deposit(argThat(command ->
				command.reason() == PointTransactionReason.SAVINGS_WITHDRAWAL && command.amount() == 400));
		}

		@Test
		@DisplayName("만기 지급 상품은 이자를 가입 건에 쌓아 두고 지갑을 건드리지 않는다")
		void 만기지급_상품은_쌓아둔다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(1).minusHours(1);
			SavingsSubscription subscription = subscription(simple, 10_000, startedAt);

			savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			// 단리예금은 전체 기간(4주) 기준 15% 를 주 단위로 나눠 쌓는다 — 한 주치는 10_000 × 15% ÷ 4.
			assertThat(subscription.getAccruedInterest()).isEqualTo(375);
			verify(pointWalletService, never()).deposit(any());
			verify(savingsTransactionRepository).save(argThat(transaction ->
				transaction.getTransactionType() == SavingsTransactionType.INTEREST));
		}

		@Test
		@DisplayName("복리예금은 지난주 이자까지 기준에 넣어 다음 이자를 키운다")
		void 복리는_이자에_이자가_붙는다() {
			SavingsProduct compound = product(SavingsProductType.COMPOUND_INTEREST);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(2).minusHours(1);
			SavingsSubscription subscription = subscription(compound, 10_000, startedAt);

			savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			// 1주차 10_000 × 10% = 1_000, 2주차 (10_000 + 1_000) × 10% = 1_100.
			assertThat(subscription.getAccruedInterest()).isEqualTo(2_100);
		}

		@Test
		@DisplayName("목표적금은 주간 정산으로 이자가 붙지 않는다")
		void 목표적금은_주간_이자가_없다() {
			SavingsProduct goal = product(SavingsProductType.GOAL_INSTALLMENT);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(1).minusHours(1);
			SavingsSubscription subscription = subscription(goal, 10_000, startedAt);

			int interest = savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			assertThat(interest).isZero();
			assertThat(subscription.getAccruedInterest()).isZero();
			verify(savingsTransactionRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("정산 주기와 만기")
	class Schedule {

		@Test
		@DisplayName("정산 시각이 아직 오지 않았으면 아무것도 하지 않는다")
		void 아직_정산일이_아니면_넘어간다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			OffsetDateTime startedAt = OffsetDateTime.now().minusDays(3);
			SavingsSubscription subscription = subscription(simple, 10_000, startedAt);

			int interest = savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			assertThat(interest).isZero();
			assertThat(subscription.getNextSettlementAt()).isEqualTo(startedAt.plusWeeks(1));
		}

		@Test
		@DisplayName("정산을 마치면 다음 정산 시각을 한 주 뒤로 옮긴다")
		void 다음_정산일을_한주_뒤로_옮긴다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(1).minusHours(1);
			SavingsSubscription subscription = subscription(simple, 10_000, startedAt);

			savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			assertThat(subscription.getNextSettlementAt()).isEqualTo(startedAt.plusWeeks(2));
		}

		@Test
		@DisplayName("밀린 주가 여러 개면 한 번에 따라잡는다")
		void 밀린_주를_한번에_따라잡는다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(3).minusHours(1);
			SavingsSubscription subscription = subscription(simple, 10_000, startedAt);

			savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			assertThat(subscription.getAccruedInterest()).isEqualTo(375 * 3);
			verify(savingsTransactionRepository, times(3)).save(any());
		}

		@Test
		@DisplayName("만기 정산까지 마치면 지급 대기 상태가 되고 정산 대상에서 빠진다")
		void 만기에_닿으면_지급대기로_바뀐다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(4).minusHours(1);
			SavingsSubscription subscription = subscription(simple, 10_000, startedAt);

			savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			// 4주 만기 상품이라 정산도 딱 4번 — 만기 총이자는 원금의 15%.
			assertThat(subscription.getAccruedInterest()).isEqualTo(1_500);
			assertThat(subscription.getStatus()).isEqualTo(SavingsSubscriptionStatus.MATURED);
			assertThat(subscription.getNextSettlementAt()).isNull();
		}

		@Test
		@DisplayName("보통예금은 만기가 없어 계속 정산된다")
		void 보통예금은_만기가_없다() {
			SavingsProduct ordinary = product(SavingsProductType.ORDINARY);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(5).minusHours(1);
			SavingsSubscription subscription = subscription(ordinary, 40_000, startedAt);
			walletAccepts();

			savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			assertThat(subscription.getStatus()).isEqualTo(SavingsSubscriptionStatus.ACTIVE);
			assertThat(subscription.getNextSettlementAt()).isEqualTo(startedAt.plusWeeks(6));
			verify(pointWalletService, times(5)).deposit(any());
		}

		@Test
		@DisplayName("이미 해지된 건은 정산하지 않는다")
		void 해지된_건은_정산하지_않는다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			OffsetDateTime startedAt = OffsetDateTime.now().minusWeeks(2);
			SavingsSubscription subscription = subscription(simple, 10_000, startedAt);
			subscription.terminate(OffsetDateTime.now());

			int interest = savingsSettlementService.settle(SUBSCRIPTION_ID, OffsetDateTime.now());

			assertThat(interest).isZero();
			assertThat(subscription.getAccruedInterest()).isZero();
		}
	}

	@Nested
	@DisplayName("정산 대상 조회")
	class DueSubscriptions {

		@Test
		@DisplayName("정산 시각이 지난 진행 중 가입 건만 고른다")
		void 정산일이_지난_진행중_건만_고른다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			OffsetDateTime now = OffsetDateTime.now();
			SavingsSubscription due = subscription(simple, 10_000, now.minusWeeks(1).minusHours(1));
			given(savingsSubscriptionRepository.findByStatusAndNextSettlementAtLessThanEqual(
				SavingsSubscriptionStatus.ACTIVE, now)).willReturn(java.util.List.of(due));

			assertThat(savingsSettlementService.findDueSubscriptionIds(now)).containsExactly(SUBSCRIPTION_ID);
		}
	}
}
