package com.bana.dotori.savings.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.notification.entity.NotificationType;
import com.bana.dotori.notification.service.PushNotificationService;
import com.bana.dotori.savings.entity.ContributionStyle;
import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.entity.SavingsTransactionType;
import com.bana.dotori.savings.repository.SavingsSubscriptionRepository;
import com.bana.dotori.savings.repository.SavingsTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 정액 적립식 예적금의 납입 리마인더.
 *
 * <p>적립식은 매주 같은 금액을 넣어야 하는데 자동이체가 없다 — 주간 정산은 이자만 계산하고,
 * 납입은 아이가 직접 눌러야 한다(SAV-04). 안 넣으면 그냥 안 들어간 채로 그 주가 지나간다.
 *
 * <p>정산일이 코앞인데 이번 주기 납입이 없는 건만 고른다. 미납이면 매일 보내는 방법도
 * 있지만 한 주에 예닐곱 번이 되어 잔소리가 된다 — 마감이 다가올 때만 알리는 편이 낫다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsContributionReminderService {

	/** 납입으로 치는 거래. 가입 때 낸 첫 회차도 그 주기의 납입이다 */
	private static final Set<SavingsTransactionType> CONTRIBUTIONS =
		Set.of(SavingsTransactionType.INITIAL_DEPOSIT, SavingsTransactionType.CONTRIBUTION);

	/** 정산일까지 이 기간 안으로 들어온 건만 알린다 */
	private static final int REMINDER_WINDOW_DAYS = 3;

	/** 주간 정산 주기. 이번 주기의 시작을 되짚는 데 쓴다 */
	private static final int SETTLEMENT_PERIOD_WEEKS = 1;

	private final SavingsSubscriptionRepository savingsSubscriptionRepository;
	private final SavingsTransactionRepository savingsTransactionRepository;
	private final PushNotificationService pushNotificationService;

	/**
	 * 미납 건에 알림을 보낸다.
	 *
	 * <p>읽기만 한다. 알림 발송 자체는 알림함 기록을 남기지만 그것은 알림 도메인의
	 * 트랜잭션이고, 여기서 예적금 상태를 바꾸지는 않는다.
	 *
	 * @return 보낸 건수
	 */
	@Transactional(readOnly = true)
	public int remindUnpaid(OffsetDateTime now) {
		List<SavingsSubscription> candidates = savingsSubscriptionRepository.findInstallmentsSettlingBy(
			SavingsSubscriptionStatus.ACTIVE, ContributionStyle.FIXED_INSTALLMENT,
			now.plusDays(REMINDER_WINDOW_DAYS));

		int sent = 0;
		for (SavingsSubscription subscription : candidates) {
			if (paidThisPeriod(subscription)) {
				continue;
			}
			pushNotificationService.notify(subscription.getChildId(), NotificationType.SAVINGS,
				"저축에 넣을 때가 됐어요", "이번 주 적금을 아직 넣지 않았어요", subscription.getId());
			sent++;
		}
		return sent;
	}

	/**
	 * 이번 주기에 납입이 있었는지.
	 *
	 * <p>주기의 시작은 다음 정산 시각에서 한 주를 뺀 지점이다. 정산이 주 단위로만 움직이므로
	 * 이렇게 되짚어도 실제 주기와 어긋나지 않는다.
	 */
	private boolean paidThisPeriod(SavingsSubscription subscription) {
		OffsetDateTime periodStart = subscription.getNextSettlementAt().minusWeeks(SETTLEMENT_PERIOD_WEEKS);
		return savingsTransactionRepository
			.existsBySavingsSubscriptionIdAndTransactionTypeInAndCreatedAtGreaterThanEqual(
				subscription.getId(), CONTRIBUTIONS, periodStart);
	}
}
