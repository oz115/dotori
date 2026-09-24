package com.bana.dotori.savings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;
import com.bana.dotori.savings.entity.ContributionStyle;
import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.entity.SavingsTransactionType;
import com.bana.dotori.savings.repository.SavingsSubscriptionRepository;
import com.bana.dotori.savings.repository.SavingsTransactionRepository;

/**
 * 납입 리마인더가 "누구를 고르는지" 를 고정한다 (S15P21A505-246).
 *
 * <p>고르는 규칙이 이 기능의 전부다. 잘못 고르면 이미 넣은 아이에게 잔소리를 하거나,
 * 정작 안 넣은 아이를 놓친다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SavingsContributionReminderServiceTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final Integer SUBSCRIPTION_ID = 3;
	private static final Integer CHILD_ID = 247;

	@Mock
	private SavingsSubscriptionRepository savingsSubscriptionRepository;

	@Mock
	private SavingsTransactionRepository savingsTransactionRepository;

	@Mock
	private PushNotificationService pushNotificationService;

	@InjectMocks
	private SavingsContributionReminderService reminderService;

	private final OffsetDateTime now = OffsetDateTime.now(KST);

	/** 정산이 이틀 뒤인 적립식 가입 건 */
	private SavingsSubscription installment() {
		OffsetDateTime startedAt = now.minusWeeks(1);
		SavingsSubscription subscription = SavingsSubscription.subscribe(96, CHILD_ID, 4, 1000, 1000, true,
			"key", startedAt, startedAt.plusWeeks(10), now.plusDays(2));
		ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
		return subscription;
	}

	private void givenCandidates(SavingsSubscription... subscriptions) {
		given(savingsSubscriptionRepository.findInstallmentsSettlingBy(eq(SavingsSubscriptionStatus.ACTIVE),
			eq(ContributionStyle.FIXED_INSTALLMENT), any())).willReturn(List.of(subscriptions));
	}

	private void givenPaidThisPeriod(boolean paid) {
		given(savingsTransactionRepository
			.existsBySavingsSubscriptionIdAndTransactionTypeInAndCreatedAtGreaterThanEqual(
				anyInt(), any(), any())).willReturn(paid);
	}

	@Test
	@DisplayName("이번 주기에 납입이 없으면 알린다")
	void 미납이면_보낸다() {
		givenCandidates(installment());
		givenPaidThisPeriod(false);

		int sent = reminderService.remindUnpaid(now);

		assertThat(sent).isEqualTo(1);
		then(pushNotificationService).should()
			.notify(eq(CHILD_ID), eq(NotificationType.SAVINGS), any(), any(), eq(SUBSCRIPTION_ID));
	}

	@Test
	@DisplayName("이미 넣었으면 보내지 않는다")
	void 납입했으면_건너뛴다() {
		givenCandidates(installment());
		givenPaidThisPeriod(true);

		int sent = reminderService.remindUnpaid(now);

		assertThat(sent).isZero();
		then(pushNotificationService).should(never()).notify(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("대상이 없으면 아무것도 보내지 않는다")
	void 대상이_없으면() {
		givenCandidates();

		int sent = reminderService.remindUnpaid(now);

		assertThat(sent).isZero();
		then(pushNotificationService).should(never()).notify(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("정산일이 창 밖이면 애초에 조회 대상이 아니다")
	void 조회는_창_안쪽만() {
		// 며칠 뒤까지 볼지는 쿼리 인자로 넘어간다. 창이 넓어지면 잔소리가 되고,
		// 좁아지면 마감을 놓친다.
		givenCandidates();

		reminderService.remindUnpaid(now);

		ArgumentCaptor<OffsetDateTime> deadline = ArgumentCaptor.forClass(OffsetDateTime.class);
		then(savingsSubscriptionRepository).should().findInstallmentsSettlingBy(
			eq(SavingsSubscriptionStatus.ACTIVE), eq(ContributionStyle.FIXED_INSTALLMENT), deadline.capture());
		assertThat(deadline.getValue()).isAfter(now);
	}

	@Test
	@DisplayName("이번 주기의 시작은 다음 정산에서 한 주를 뺀 지점이다")
	void 주기_시작을_되짚는다() {
		// 가입 때 낸 첫 회차도 그 주기의 납입으로 친다.
		SavingsSubscription subscription = installment();
		givenCandidates(subscription);
		givenPaidThisPeriod(false);

		reminderService.remindUnpaid(now);

		ArgumentCaptor<OffsetDateTime> periodStart = ArgumentCaptor.forClass(OffsetDateTime.class);
		ArgumentCaptor<Collection<SavingsTransactionType>> types = ArgumentCaptor.forClass(Collection.class);
		then(savingsTransactionRepository).should()
			.existsBySavingsSubscriptionIdAndTransactionTypeInAndCreatedAtGreaterThanEqual(
				eq(SUBSCRIPTION_ID), types.capture(), periodStart.capture());

		assertThat(periodStart.getValue())
			.isEqualTo(subscription.getNextSettlementAt().minusWeeks(1));
		assertThat(types.getValue())
			.containsExactlyInAnyOrder(SavingsTransactionType.INITIAL_DEPOSIT, SavingsTransactionType.CONTRIBUTION);
	}
}
