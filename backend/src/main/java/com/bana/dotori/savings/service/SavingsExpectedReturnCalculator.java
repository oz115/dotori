package com.bana.dotori.savings.service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.bana.dotori.savings.entity.ContributionStyle;
import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsProductType;
import com.bana.dotori.savings.entity.SavingsSubscription;

import lombok.RequiredArgsConstructor;

/**
 * SAV-05 예상 손익 — "지금 이대로 두면 만기에 얼마를 받는가"를 미리 계산한다.
 *
 * <p>정산이 실제로 하는 계산({@link SavingsProductType#weeklyInterest})을 남은 주만큼 앞당겨
 * 돌려볼 뿐이다. 계산을 따로 구현하면 화면에 보여준 예상과 나중에 실제로 들어온 금액이 어긋난다.
 *
 * <p><b>가정.</b> 예상이므로 아이가 약속을 지킨다고 본다.
 * <ul>
 *   <li>정액 적립식은 남은 회차를 매주 그대로 납입한다.</li>
 * </ul>
 * 약속이 깨지면 실제 이자는 이보다 줄어든다 — 그 편차 자체가 이 상품들이 가르치려는 것이다.
 */
@Component
@RequiredArgsConstructor
public class SavingsExpectedReturnCalculator {

	/**
	 * 만기까지 더 쌓일 이자. 이미 쌓인 {@code accruedInterest} 는 포함하지 않는다.
	 *
	 * <p>보통예금은 만기가 없어서 "만기 총이자"라는 것이 없다. 대신 <b>다음 정산 한 주치</b>를
	 * 돌려준다 — 가입 안내 문구가 약속하는 것("현재 금액을 다음 주까지 유지하면 이자를 받아요")과
	 * 같은 값이라, 아이가 화면에서 보는 숫자가 일관된다.
	 */
	public long remainingInterest(SavingsSubscription subscription, SavingsProduct product) {
		if (!subscription.isActive()) {
			// 이미 만기·해지된 건은 더 쌓일 것이 없다.
			return 0L;
		}

		SavingsProductType type = product.getProductType();
		if (type.paysInterestImmediately()) {
			return type.weeklyInterest(subscription.getCurrentAmount(), 0);
		}

		return accrueRemainingWeeks(subscription, product);
	}

	/** 지금 중도 해지하면 받는 금액. 쌓인 이자는 포기하므로 납입 원금 그대로다 */
	public long earlyTerminationAmount(SavingsSubscription subscription) {
		return subscription.getCurrentAmount();
	}

	/**
	 * 남은 정산 회차만큼 이자를 앞당겨 쌓아 본다. 정산({@code SavingsSettlementService})과 같은
	 * 순서다 — 그 주의 이자를 먼저 쌓고, 남은 회차가 있으면 다음 주 납입으로 원금을 늘린다.
	 */
	private long accrueRemainingWeeks(SavingsSubscription subscription, SavingsProduct product) {
		SavingsProductType type = product.getProductType();
		long balance = subscription.getCurrentAmount();
		long accrued = subscription.getAccruedInterest();
		long before = accrued;

		int remainingSettlements = remainingSettlements(subscription, product);
		int remainingInstallments = remainingInstallments(subscription, product);

		for (int week = 1; week <= remainingSettlements; week++) {
			accrued += type.weeklyInterest(balance, accrued);
			if (remainingInstallments > 0) {
				balance += subscription.getPrincipal();
				remainingInstallments--;
			}
		}
		return accrued - before;
	}

	/**
	 * 앞으로 남은 정산 횟수.
	 *
	 * <p>가입 시각과 다음 정산 시각의 간격으로 이미 몇 번 정산했는지 역산한다
	 * ({@code nextSettlementAt = startedAt + (지난 정산 횟수 + 1)주}). 정산 이력을 따로 세지 않고도
	 * 알 수 있고, 정산이 밀려 있으면 그만큼 남은 횟수가 많게 잡혀 실제와 맞는다.
	 */
	private int remainingSettlements(SavingsSubscription subscription, SavingsProduct product) {
		Integer termWeeks = product.getTermWeeks();
		OffsetDateTime nextSettlementAt = subscription.getNextSettlementAt();
		if (termWeeks == null || nextSettlementAt == null) {
			return 0;
		}

		long settled = ChronoUnit.WEEKS.between(subscription.getStartedAt(), nextSettlementAt) - 1;
		return (int)Math.max(0, termWeeks - Math.max(0, settled));
	}

	/** 앞으로 더 넣기로 한 납입 회차. 정액 적립식이 아니면 0 */
	private int remainingInstallments(SavingsSubscription subscription, SavingsProduct product) {
		if (product.getContributionStyle() != ContributionStyle.FIXED_INSTALLMENT) {
			return 0;
		}
		Integer installmentCount = product.getInstallmentCount();
		if (installmentCount == null) {
			return 0;
		}
		return Math.max(0, installmentCount - subscription.getContributionCount());
	}
}
