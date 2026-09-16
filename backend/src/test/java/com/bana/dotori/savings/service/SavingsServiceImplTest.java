package com.bana.dotori.savings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.family.entity.FinancialKnowledgeLevel;
import com.bana.dotori.family.service.FamilySettingsReader;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.savings.dto.request.SavingsContributeRequest;
import com.bana.dotori.savings.dto.request.SavingsSubscribeRequest;
import com.bana.dotori.savings.dto.response.SavingsProductResponse;
import com.bana.dotori.savings.dto.response.SavingsSubscriptionResponse;
import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.entity.SavingsTransaction;
import com.bana.dotori.savings.entity.SavingsTransactionType;
import com.bana.dotori.savings.mapper.SavingsMapper;
import com.bana.dotori.savings.repository.SavingsProductRepository;
import com.bana.dotori.savings.repository.SavingsSubscriptionRepository;
import com.bana.dotori.savings.repository.SavingsTransactionRepository;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointWalletService;

/**
 * 2계층 - 가입·납입·수령·해지의 판정 규칙과 지갑 호출 규칙만 검증한다.
 *
 * <p>가족·아이 경계는 {@link SavingsAccessValidator}, 실제 잔액 변경은
 * {@link PointWalletService} 몫이라 둘 다 모킹한다. 시간이 흘러 일어나는 이자 적립은
 * {@link SavingsSettlementServiceTest} 가 맡는다.
 */
@DisplayName("SavingsServiceImpl 가입·납입·수령·해지")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SavingsServiceImplTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer PRODUCT_ID = 1;
	private static final Integer SUBSCRIPTION_ID = 77;
	private static final int WEEKLY_REWARD_LIMIT = 10_000;
	private static final String IDEMPOTENCY_KEY = "idem-key";

	@Mock
	private SavingsProductRepository savingsProductRepository;

	@Mock
	private SavingsSubscriptionRepository savingsSubscriptionRepository;

	@Mock
	private SavingsTransactionRepository savingsTransactionRepository;

	@Mock
	private SavingsAccessValidator savingsAccessValidator;

	@Mock
	private FamilySettingsReader familySettingsReader;

	@Mock
	private PointWalletService pointWalletService;

	@Spy
	private SavingsMapper savingsMapper;

	@InjectMocks
	private SavingsServiceImpl savingsService;

	private static FamilySettings settings() {
		return new FamilySettings(0, WEEKLY_REWARD_LIMIT, FinancialKnowledgeLevel.BEGINNER);
	}

	private static long maxAmountOf(SavingsProductType type) {
		return type.calculateMaxAmount(settings());
	}

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

	/** 가입한 지 {@code weeksAgo} 주 지난 진행 중인 건 */
	private SavingsSubscription existingSubscription(SavingsProduct product, int principal, int currentAmount,
		int contributionCount, int weeksAgo) {
		SavingsSubscription subscription = existingSubscription(product, principal, currentAmount, contributionCount);
		ReflectionTestUtils.setField(subscription, "startedAt", OffsetDateTime.now().minusWeeks(weeksAgo));
		return subscription;
	}

	/** 이미 가입돼 진행 중인 건을 만들어 잠금 조회에 물려 둔다 */
	private SavingsSubscription existingSubscription(SavingsProduct product, int principal, int currentAmount,
		int contributionCount) {
		SavingsSubscription subscription = SavingsSubscription.subscribe(FAMILY_ID, CHILD_ID, PRODUCT_ID, principal,
			currentAmount, contributionCount > 0, IDEMPOTENCY_KEY, OffsetDateTime.now(),
			product.getTermWeeks() == null ? null : OffsetDateTime.now().plusWeeks(product.getTermWeeks()),
			OffsetDateTime.now().plusWeeks(1));
		ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
		ReflectionTestUtils.setField(subscription, "contributionCount", contributionCount);
		given(savingsSubscriptionRepository.findByIdAndFamilyIdAndChildIdForUpdate(SUBSCRIPTION_ID, FAMILY_ID,
			CHILD_ID)).willReturn(Optional.of(subscription));
		return subscription;
	}

	private static PointChangeCommand reasonIs(PointTransactionReason reason) {
		return argThat(command -> command != null && command.reason() == reason);
	}

	private static PointChangeResult applied(int amount) {
		return new PointChangeResult(1, CHILD_ID, PointTransactionReason.SAVINGS_DEPOSIT, amount, 0,
			OffsetDateTime.now(), false);
	}

	@BeforeEach
	void setUp() {
		given(familySettingsReader.read(FAMILY_ID, CHILD_ID)).willReturn(settings());
		given(pointWalletService.withdraw(any())).willReturn(applied(0));
		given(pointWalletService.deposit(any())).willReturn(applied(0));
	}

	@Nested
	@DisplayName("상품 목록 조회")
	class ListProducts {

		@Test
		@DisplayName("활성 상품마다 이 가족의 최대 가입 금액을 계산해 응답한다")
		void 활성_상품마다_최대금액을_계산한다() {
			// product(...) 안에서 findById 를 스텁하므로, 다른 스텁을 시작하기 전에 먼저 만들어 둔다.
			SavingsProduct ordinary = product(SavingsProductType.ORDINARY);
			given(savingsProductRepository.findByActiveIsTrue()).willReturn(List.of(ordinary));

			List<SavingsProductResponse> responses = savingsService.listProducts(CHILD_ID, FAMILY_ID, CHILD_ID);

			assertThat(responses).hasSize(1);
			assertThat(responses.get(0).maxAmount()).isEqualTo(maxAmountOf(SavingsProductType.ORDINARY));
		}
	}

	@Nested
	@DisplayName("가입")
	class Subscribe {

		private SavingsSubscribeRequest requestOf(int amount) {
			return new SavingsSubscribeRequest(PRODUCT_ID, amount);
		}

		@Test
		@DisplayName("한도 이내 금액이면 지갑에서 차감하고 가입을 저장한다")
		void 한도_이내면_가입에_성공한다() {
			product(SavingsProductType.ORDINARY);
			int amount = (int)maxAmountOf(SavingsProductType.ORDINARY);

			SavingsSubscriptionResponse response = savingsService.subscribe(CHILD_ID, FAMILY_ID, CHILD_ID,
				requestOf(amount), IDEMPOTENCY_KEY);

			verify(pointWalletService).createWalletIfAbsent(CHILD_ID);
			verify(pointWalletService).withdraw(reasonIs(PointTransactionReason.SAVINGS_DEPOSIT));
			verify(savingsSubscriptionRepository).saveAndFlush(any(SavingsSubscription.class));
			assertThat(response.principal()).isEqualTo(amount);
			assertThat(response.currentAmount()).isEqualTo(amount);
			assertThat(response.productType()).isEqualTo(SavingsProductType.ORDINARY);
		}

		@Test
		@DisplayName("같은 멱등키로 다시 오면 기존 가입을 그대로 돌려주고 지갑을 건드리지 않는다")
		void 같은_멱등키면_기존_가입을_돌려준다() {
			SavingsProduct ordinary = product(SavingsProductType.ORDINARY);
			SavingsSubscription already = SavingsSubscription.subscribe(FAMILY_ID, CHILD_ID, PRODUCT_ID, 1_000,
				1_000, false, IDEMPOTENCY_KEY, OffsetDateTime.now(), null, OffsetDateTime.now().plusWeeks(1));
			ReflectionTestUtils.setField(already, "id", SUBSCRIPTION_ID);
			given(savingsSubscriptionRepository.findByChildIdAndIdempotencyKey(CHILD_ID, IDEMPOTENCY_KEY))
				.willReturn(Optional.of(already));

			SavingsSubscriptionResponse response = savingsService.subscribe(CHILD_ID, FAMILY_ID, CHILD_ID,
				requestOf(1_000), IDEMPOTENCY_KEY);

			assertThat(response.savingsSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
			assertThat(response.productName()).isEqualTo(ordinary.getName());
			verify(pointWalletService, never()).withdraw(any());
			verify(savingsSubscriptionRepository, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("목표적금은 목표 금액만 정하고 가입 시 차감하지 않는다")
		void 목표적금은_가입시_차감하지_않는다() {
			product(SavingsProductType.GOAL_INSTALLMENT);
			int goal = (int)maxAmountOf(SavingsProductType.GOAL_INSTALLMENT);

			SavingsSubscriptionResponse response = savingsService.subscribe(CHILD_ID, FAMILY_ID, CHILD_ID,
				requestOf(goal), IDEMPOTENCY_KEY);

			assertThat(response.principal()).isEqualTo(goal);
			assertThat(response.currentAmount()).isZero();
			verify(pointWalletService, never()).withdraw(any());
		}

		@Test
		@DisplayName("정액 적립식은 가입 시 최초 납입이 1회차로 잡힌다")
		void 정액적립식은_최초납입이_1회차다() {
			product(SavingsProductType.SMALL_INSTALLMENT);
			int weekly = (int)maxAmountOf(SavingsProductType.SMALL_INSTALLMENT);

			SavingsSubscriptionResponse response = savingsService.subscribe(CHILD_ID, FAMILY_ID, CHILD_ID,
				requestOf(weekly), IDEMPOTENCY_KEY);

			assertThat(response.contributionCount()).isEqualTo(1);
			assertThat(response.currentAmount()).isEqualTo(weekly);
		}

		@Test
		@DisplayName("한도를 넘으면 거부하고 지갑을 건드리지 않는다")
		void 한도_초과면_거부한다() {
			product(SavingsProductType.ORDINARY);
			int overLimit = (int)maxAmountOf(SavingsProductType.ORDINARY) + 1;

			assertThatThrownBy(() -> savingsService.subscribe(CHILD_ID, FAMILY_ID, CHILD_ID,
				requestOf(overLimit), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_AMOUNT_EXCEEDS_LIMIT);

			verify(pointWalletService, never()).withdraw(any());
			verify(savingsSubscriptionRepository, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("상품이 없으면 거부한다")
		void 상품이_없으면_거부한다() {
			given(savingsProductRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

			assertThatThrownBy(() -> savingsService.subscribe(CHILD_ID, FAMILY_ID, CHILD_ID,
				requestOf(1_000), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_PRODUCT_NOT_FOUND);
		}

		@Test
		@DisplayName("가입 중단된 상품이면 거부한다")
		void 중단된_상품이면_거부한다() {
			SavingsProduct inactive = product(SavingsProductType.ORDINARY);
			ReflectionTestUtils.setField(inactive, "active", false);

			assertThatThrownBy(() -> savingsService.subscribe(CHILD_ID, FAMILY_ID, CHILD_ID,
				requestOf(1_000), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_PRODUCT_INACTIVE);
		}
	}

	@Nested
	@DisplayName("추가 납입")
	class Contribute {

		@Test
		@DisplayName("정액 적립식은 약정액과 같은 금액만 받는다")
		void 약정액과_다르면_거부한다() {
			SavingsProduct small = product(SavingsProductType.SMALL_INSTALLMENT);
			existingSubscription(small, 1_000, 1_000, 1);

			assertThatThrownBy(() -> savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				new SavingsContributeRequest(999), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_INVALID_CONTRIBUTION_AMOUNT);

			verify(pointWalletService, never()).withdraw(any());
		}

		@Test
		@DisplayName("정액 적립식은 약속한 회차를 넘겨 납입할 수 없다")
		void 회차를_넘기면_거부한다() {
			SavingsProduct small = product(SavingsProductType.SMALL_INSTALLMENT);
			existingSubscription(small, 1_000, 2_000, 2);

			assertThatThrownBy(() -> savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				new SavingsContributeRequest(1_000), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_CONTRIBUTION_NOT_ALLOWED);
		}

		@Test
		@DisplayName("정액 적립식은 한 주에 두 번 납입할 수 없다")
		void 같은_주에_두번_납입할_수_없다() {
			SavingsProduct small = product(SavingsProductType.SMALL_INSTALLMENT);
			// 가입(1회차)한 그 주에 곧바로 2회차를 넣으려는 상황.
			existingSubscription(small, 1_000, 1_000, 1, 0);

			assertThatThrownBy(() -> savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				new SavingsContributeRequest(1_000), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_CONTRIBUTION_NOT_ALLOWED);

			verify(pointWalletService, never()).withdraw(any());
		}

		@Test
		@DisplayName("한 주가 지나면 다음 회차를 납입할 수 있다")
		void 한주_뒤에는_다음_회차를_납입한다() {
			SavingsProduct small = product(SavingsProductType.SMALL_INSTALLMENT);
			existingSubscription(small, 1_000, 1_000, 1, 1);

			SavingsSubscriptionResponse response = savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID,
				SUBSCRIPTION_ID, new SavingsContributeRequest(1_000), IDEMPOTENCY_KEY);

			assertThat(response.currentAmount()).isEqualTo(2_000);
			assertThat(response.contributionCount()).isEqualTo(2);
			verify(pointWalletService).withdraw(reasonIs(PointTransactionReason.SAVINGS_DEPOSIT));
		}

		@Test
		@DisplayName("거치식(단리·복리)은 추가 납입 자체가 없다")
		void 거치식은_추가납입을_거부한다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			existingSubscription(simple, 1_000, 1_000, 0);

			assertThatThrownBy(() -> savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				new SavingsContributeRequest(1_000), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_CONTRIBUTION_NOT_ALLOWED);
		}

		@Test
		@DisplayName("목표적금은 목표 금액을 넘겨 납입할 수 없다")
		void 목표를_넘기면_거부한다() {
			SavingsProduct goal = product(SavingsProductType.GOAL_INSTALLMENT);
			existingSubscription(goal, 10_000, 9_000, 3);

			assertThatThrownBy(() -> savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				new SavingsContributeRequest(1_001), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_INVALID_CONTRIBUTION_AMOUNT);
		}

		@Test
		@DisplayName("목표를 채우면 그 자리에서 이자를 얹어 지급하고 수령 완료가 된다")
		void 목표를_채우면_즉시_지급한다() {
			SavingsProduct goal = product(SavingsProductType.GOAL_INSTALLMENT);
			SavingsSubscription subscription = existingSubscription(goal, 10_000, 9_000, 3);

			SavingsSubscriptionResponse response = savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID,
				SUBSCRIPTION_ID, new SavingsContributeRequest(1_000), IDEMPOTENCY_KEY);

			long expectedInterest = SavingsProductType.GOAL_INSTALLMENT.goalAchievementInterest(10_000);
			assertThat(response.status()).isEqualTo(SavingsSubscriptionStatus.COMPLETED);
			assertThat(response.accruedInterest()).isEqualTo((int)expectedInterest);
			assertThat(response.totalAmount()).isEqualTo(10_000 + (int)expectedInterest);
			assertThat(subscription.getReceivedAt()).isNotNull();
			verify(pointWalletService).deposit(reasonIs(PointTransactionReason.SAVINGS_WITHDRAWAL));
			verifyRecorded(SavingsTransactionType.MATURITY_PAYOUT);
		}

		@Test
		@DisplayName("목표에 못 미치는 납입은 진행 중 상태 그대로 둔다")
		void 목표_미달이면_계속_진행한다() {
			SavingsProduct goal = product(SavingsProductType.GOAL_INSTALLMENT);
			existingSubscription(goal, 10_000, 3_000, 1);

			SavingsSubscriptionResponse response = savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID,
				SUBSCRIPTION_ID, new SavingsContributeRequest(2_000), IDEMPOTENCY_KEY);

			assertThat(response.status()).isEqualTo(SavingsSubscriptionStatus.ACTIVE);
			assertThat(response.currentAmount()).isEqualTo(5_000);
			assertThat(response.contributionCount()).isEqualTo(2);
			verify(pointWalletService, never()).deposit(any());
		}

		@Test
		@DisplayName("지갑이 이미 처리된 요청이라고 하면 납입 전체를 되돌린다")
		void 지갑이_중복이라고_하면_거부한다() {
			SavingsProduct goal = product(SavingsProductType.GOAL_INSTALLMENT);
			existingSubscription(goal, 10_000, 1_000, 1);
			given(pointWalletService.withdraw(any())).willReturn(
				new PointChangeResult(1, CHILD_ID, PointTransactionReason.SAVINGS_DEPOSIT, 1_000, 0,
					OffsetDateTime.now(), true));

			assertThatThrownBy(() -> savingsService.contribute(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				new SavingsContributeRequest(1_000), IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.DUPLICATE_POINT_REQUEST);
		}
	}

	@Nested
	@DisplayName("만기 수령")
	class Receive {

		@Test
		@DisplayName("만기 전이면 거부한다")
		void 만기_전이면_거부한다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			existingSubscription(simple, 1_000, 1_000, 0);

			assertThatThrownBy(() -> savingsService.receive(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_NOT_MATURED);

			verify(pointWalletService, never()).deposit(any());
		}

		@Test
		@DisplayName("만기 도달 건은 원금과 쌓인 이자를 함께 지급한다")
		void 만기_건은_원금과_이자를_지급한다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			SavingsSubscription subscription = existingSubscription(simple, 1_000, 1_000, 0);
			subscription.accrueInterest(150);
			subscription.markMatured();

			SavingsSubscriptionResponse response = savingsService.receive(CHILD_ID, FAMILY_ID, CHILD_ID,
				SUBSCRIPTION_ID, IDEMPOTENCY_KEY);

			assertThat(response.status()).isEqualTo(SavingsSubscriptionStatus.COMPLETED);
			assertThat(response.totalAmount()).isEqualTo(1_150);
			verify(pointWalletService).deposit(argThat(command ->
				command.reason() == PointTransactionReason.SAVINGS_WITHDRAWAL && command.amount() == 1_150));
			verifyRecorded(SavingsTransactionType.MATURITY_PAYOUT);
		}

		@Test
		@DisplayName("이미 수령한 건은 다시 받을 수 없다")
		void 이미_수령했으면_거부한다() {
			SavingsProduct simple = product(SavingsProductType.SIMPLE_INTEREST);
			SavingsSubscription subscription = existingSubscription(simple, 1_000, 1_000, 0);
			subscription.markMatured();
			subscription.complete(OffsetDateTime.now());

			assertThatThrownBy(() -> savingsService.receive(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_NOT_MATURED);
		}
	}

	@Nested
	@DisplayName("중도 해지")
	class Terminate {

		@Test
		@DisplayName("납입 원금만 돌려주고 쌓인 이자는 지급하지 않는다")
		void 원금만_돌려준다() {
			SavingsProduct small = product(SavingsProductType.SMALL_INSTALLMENT);
			SavingsSubscription subscription = existingSubscription(small, 1_000, 2_000, 2);
			subscription.accrueInterest(100);

			SavingsSubscriptionResponse response = savingsService.terminate(CHILD_ID, FAMILY_ID, CHILD_ID,
				SUBSCRIPTION_ID, IDEMPOTENCY_KEY);

			assertThat(response.status()).isEqualTo(SavingsSubscriptionStatus.CANCELLED);
			assertThat(response.accruedInterest()).isZero();
			verify(pointWalletService).deposit(argThat(command ->
				command.reason() == PointTransactionReason.SAVINGS_WITHDRAWAL && command.amount() == 2_000));
			verifyRecorded(SavingsTransactionType.EARLY_TERMINATION_PAYOUT);
		}

		@Test
		@DisplayName("이미 끝난 건은 해지할 수 없다")
		void 끝난_건은_해지할_수_없다() {
			SavingsProduct small = product(SavingsProductType.SMALL_INSTALLMENT);
			SavingsSubscription subscription = existingSubscription(small, 1_000, 2_000, 2);
			subscription.terminate(OffsetDateTime.now());

			assertThatThrownBy(() -> savingsService.terminate(CHILD_ID, FAMILY_ID, CHILD_ID, SUBSCRIPTION_ID,
				IDEMPOTENCY_KEY))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_ACTIVE);
		}

		@Test
		@DisplayName("아직 한 푼도 납입하지 않은 목표적금은 지갑을 건드리지 않고 해지된다")
		void 납입이_없으면_지갑을_건드리지_않는다() {
			SavingsProduct goal = product(SavingsProductType.GOAL_INSTALLMENT);
			existingSubscription(goal, 10_000, 0, 0);

			SavingsSubscriptionResponse response = savingsService.terminate(CHILD_ID, FAMILY_ID, CHILD_ID,
				SUBSCRIPTION_ID, IDEMPOTENCY_KEY);

			assertThat(response.status()).isEqualTo(SavingsSubscriptionStatus.CANCELLED);
			verify(pointWalletService, never()).deposit(any());
		}
	}

	@Nested
	@DisplayName("가입 내역 상세 조회")
	class GetSubscription {

		@Test
		@DisplayName("존재하지 않으면 거부한다")
		void 존재하지_않으면_거부한다() {
			given(savingsSubscriptionRepository.findByIdAndFamilyIdAndChildId(99, FAMILY_ID, CHILD_ID))
				.willReturn(Optional.empty());

			assertThatThrownBy(() -> savingsService.getSubscription(CHILD_ID, FAMILY_ID, CHILD_ID, 99))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_FOUND);
		}
	}

	/** 해당 종류의 예적금 거래 이력이 남았는지 본다 */
	private void verifyRecorded(SavingsTransactionType type) {
		verify(savingsTransactionRepository).save(argThat(transaction ->
			transaction != null && transaction.getTransactionType() == type));
	}
}
