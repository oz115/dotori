package com.bana.dotori.savings.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.entity.SavingsTransaction;
import com.bana.dotori.savings.entity.SavingsTransactionType;
import com.bana.dotori.savings.repository.SavingsProductRepository;
import com.bana.dotori.savings.repository.SavingsSubscriptionRepository;
import com.bana.dotori.savings.repository.SavingsTransactionRepository;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointWalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주간 정산 — 시간이 흘러서 저절로 일어나는 일만 여기서 처리한다. 이자를 쌓고(또는 지급하고),
 * 만기에 닿았으면 지급 대기 상태로 바꾼다. 아이가 눌러서 일어나는 일(납입·수령·해지)은
 * {@link SavingsServiceImpl} 에 있다.
 *
 * <p><b>정산일은 가입일 기준이다.</b> 모두를 월요일에 맞추면 일요일에 가입한 아이가 하루 만에
 * 한 주치 이자를 받고, 월요일에 가입한 아이는 이레를 기다린다. 가입 시각에 주 단위로 더해
 * 각자의 주기를 갖게 하면 그 불공평이 없다.
 *
 * <p><b>한 건의 정산은 그 건만의 트랜잭션이다.</b> 한 아이의 정산이 실패해도 나머지 가족의
 * 정산은 그대로 커밋된다. 실패한 건은 {@code next_settlement_at} 이 그대로 남아 다음 날 다시
 * 대상이 되므로, 놓치는 것이 아니라 미뤄지는 것이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsSettlementService {

	private final SavingsSubscriptionRepository savingsSubscriptionRepository;
	private final SavingsProductRepository savingsProductRepository;
	private final SavingsTransactionRepository savingsTransactionRepository;
	private final PointWalletService pointWalletService;

	/** 지금 정산해야 하는 가입 건의 id. 건마다 트랜잭션을 나누기 위해 id 만 먼저 모은다 */
	@Transactional(readOnly = true)
	public List<Integer> findDueSubscriptionIds(OffsetDateTime now) {
		return savingsSubscriptionRepository
			.findByStatusAndNextSettlementAtLessThanEqual(SavingsSubscriptionStatus.ACTIVE, now).stream()
			.map(SavingsSubscription::getId)
			.toList();
	}

	/**
	 * 가입 건 하나를 지금 시각까지 정산한다.
	 *
	 * <p>밀린 주가 여러 개면 한 번에 따라잡는다 — 서버가 며칠 멈춰 있었다고 해서 그 주의 이자가
	 * 사라지면 안 되기 때문이다. 이자는 그 주의 정산 시각 기준으로 계산하므로, 늦게 돌아도
	 * 제때 돌았을 때와 결과가 같다.
	 *
	 * @return 이번 호출로 쌓(거나 지급)한 이자 합계
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int settle(Integer savingsSubscriptionId, OffsetDateTime now) {
		SavingsSubscription subscription = savingsSubscriptionRepository.findByIdForUpdate(savingsSubscriptionId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_FOUND,
				"savingsSubscriptionId=" + savingsSubscriptionId));

		// 락을 기다리는 사이에 아이가 해지·수령을 끝냈을 수 있다. 락을 잡은 뒤에 다시 본다.
		if (!subscription.isActive() || subscription.getNextSettlementAt() == null) {
			return 0;
		}

		SavingsProduct product = savingsProductRepository.findById(subscription.getSavingsProductId())
			.orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_PRODUCT_NOT_FOUND,
				"savingsProductId=" + subscription.getSavingsProductId()));

		int total = 0;
		while (subscription.isActive() && isDue(subscription, now)) {
			total += settleOneWeek(subscription, product);
		}
		return total;
	}

	private boolean isDue(SavingsSubscription subscription, OffsetDateTime now) {
		OffsetDateTime next = subscription.getNextSettlementAt();
		return next != null && !next.isAfter(now);
	}

	private int settleOneWeek(SavingsSubscription subscription, SavingsProduct product) {
		OffsetDateTime settledAt = subscription.getNextSettlementAt();
		int interest = Math.toIntExact(product.getProductType()
			.weeklyInterest(subscription.getCurrentAmount(), subscription.getAccruedInterest()));

		if (interest > 0) {
			boolean recorded = true;
			if (product.getProductType().paysInterestImmediately()) {
				recorded = payInterestNow(subscription, product, interest, settledAt);
			} else {
				subscription.accrueInterest(interest);
			}
			if (recorded) {
				savingsTransactionRepository.save(
					SavingsTransaction.record(subscription.getId(), SavingsTransactionType.INTEREST, interest));
			}
		}

		OffsetDateTime maturityAt = subscription.getMaturityAt();
		if (maturityAt != null && !settledAt.isBefore(maturityAt)) {
			// 만기 정산까지 마쳤다. 지급은 아이가 수령을 눌러야 일어난다.
			subscription.markMatured();
			log.info("예적금 만기 도달. savingsSubscriptionId={}, 원금={}, 이자={}",
				subscription.getId(), subscription.getCurrentAmount(), subscription.getAccruedInterest());
		} else {
			subscription.scheduleNextSettlement(settledAt.plusWeeks(1));
		}

		return interest;
	}

	/**
	 * 보통예금 — 이자를 쌓지 않고 그 자리에서 지갑으로 보낸다.
	 *
	 * <p>멱등키를 정산 회차로 못 박아 둔다. 스케줄러가 두 번 돌거나 수동으로 다시 돌려도 같은 주의
	 * 이자가 두 번 들어가지 않는다 — {@code point_transactions} 의 UNIQUE(child_id, idempotency_key)
	 * 가 막아 준다. 이미 지급된 회차면 지갑이 이전 거래를 돌려주는데, 그때는 이력도 남기지 않는다
	 * — 포인트는 한 번 들어갔는데 이자 이력만 두 줄이 되면 만기 지급액을 설명할 수 없다.
	 *
	 * @return 이번 호출로 실제 지급됐으면 true
	 */
	private boolean payInterestNow(SavingsSubscription subscription, SavingsProduct product, int interest,
		OffsetDateTime settledAt) {
		String idempotencyKey = "savings-interest:" + subscription.getId() + ":" + settledAt.toLocalDate();

		pointWalletService.createWalletIfAbsent(subscription.getChildId());
		PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(subscription.getChildId(),
			interest, PointTransactionReason.SAVINGS_WITHDRAWAL, product.getName() + " 이자", idempotencyKey));

		if (result.replayed()) {
			log.info("이미 지급된 정산 회차라 이자 지급을 건너뛴다. savingsSubscriptionId={}, 정산일={}",
				subscription.getId(), settledAt.toLocalDate());
			return false;
		}
		return true;
	}
}
