package com.bana.dotori.savings.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가족·아이별 예적금 가입 기록. {@code principal} 은 가입 시점에 확정한 금액 스냅샷이다
 * (거치식=최초원금, 정액 적립식=주당 약정액) — 이후 가족의
 * {@code weeklyRewardLimit} 이 바뀌어도 이미 가입된 건은 영향받지 않는다.
 *
 * <p><b>원금과 이자를 나눠 들고 있다.</b> {@code currentAmount} 는 납입된 원금 누계,
 * {@code accruedInterest} 는 정산으로 쌓인 이자 누계다. 섞어서 한 컬럼으로 두면 복리예금은
 * 맞지만("이자에 이자") 단리·적립식은 틀린 이자가 나온다. 나눠 두면 복리는 두 값을 더한 것을
 * 기준으로 삼기만 하면 되고({@link InterestAccrual#WEEKLY_COMPOUND}), 만기 지급액은 모든 상품이
 * 똑같이 {@link #payoutAmount()} 가 된다.
 *
 * <p>상태 전이는 전부 이 클래스의 메서드를 거친다. ACTIVE → MATURED → COMPLETED 가 정상 경로이고,
 * ACTIVE 에서 곧바로 CANCELLED 로 가는 것이 중도 해지다. 어긋난 전이는 여기서 409 로 막는다.
 */
@Entity
@Table(name = "savings_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingsSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "savings_subscription_id")
	private Integer id;

	@Column(name = "family_id", nullable = false, updatable = false)
	private Integer familyId;

	@Column(name = "child_id", nullable = false, updatable = false)
	private Integer childId;

	@Column(name = "savings_product_id", nullable = false, updatable = false)
	private Integer savingsProductId;

	@Column(name = "principal", nullable = false, updatable = false)
	private int principal;

	@Column(name = "current_amount", nullable = false)
	private int currentAmount;

	@Column(name = "accrued_interest", nullable = false)
	private int accruedInterest;

	@Column(name = "contribution_count", nullable = false)
	private int contributionCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private SavingsSubscriptionStatus status;

	@Column(name = "idempotency_key", length = 100, updatable = false)
	private String idempotencyKey;

	@Column(name = "started_at", nullable = false, updatable = false)
	private OffsetDateTime startedAt;

	@Column(name = "maturity_at")
	private OffsetDateTime maturityAt;

	@Column(name = "next_settlement_at")
	private OffsetDateTime nextSettlementAt;

	@Column(name = "terminated_at")
	private OffsetDateTime terminatedAt;

	@Column(name = "received_at")
	private OffsetDateTime receivedAt;

	private SavingsSubscription(Integer familyId, Integer childId, Integer savingsProductId, int principal,
		int initialDeposit, boolean initialDepositIsInstallment, String idempotencyKey, OffsetDateTime startedAt,
		OffsetDateTime maturityAt, OffsetDateTime nextSettlementAt) {
		this.familyId = familyId;
		this.childId = childId;
		this.savingsProductId = savingsProductId;
		this.principal = principal;
		this.currentAmount = initialDeposit;
		this.accruedInterest = 0;
		this.contributionCount = initialDepositIsInstallment ? 1 : 0;
		this.status = SavingsSubscriptionStatus.ACTIVE;
		this.idempotencyKey = idempotencyKey;
		this.startedAt = startedAt;
		this.maturityAt = maturityAt;
		this.nextSettlementAt = nextSettlementAt;
	}

	/**
	 * 새 가입을 만든다.
	 *
	 * @param principal                   가입 시점에 확정한 금액 스냅샷
	 * @param initialDeposit              가입과 동시에 실제로 납입되는 금액
	 * @param initialDepositIsInstallment 그 최초 납입을 1회차로 셀 것인가 (정액 적립식)
	 * @param maturityAt                  만기 시각. 보통예금(무기한)은 null
	 */
	public static SavingsSubscription subscribe(Integer familyId, Integer childId, Integer savingsProductId,
		int principal, int initialDeposit, boolean initialDepositIsInstallment, String idempotencyKey,
		OffsetDateTime startedAt, OffsetDateTime maturityAt, OffsetDateTime nextSettlementAt) {
		return new SavingsSubscription(familyId, childId, savingsProductId, principal, initialDeposit,
			initialDepositIsInstallment, idempotencyKey, startedAt, maturityAt, nextSettlementAt);
	}

	/** 만기(또는 해지) 때 지갑으로 나갈 금액 — 납입 원금 누계 + 쌓인 이자 */
	public int payoutAmount() {
		return currentAmount + accruedInterest;
	}

	public boolean isActive() {
		return status == SavingsSubscriptionStatus.ACTIVE;
	}

	/** 추가 납입을 반영한다. 금액 검증은 상품 규칙을 아는 서비스가 먼저 끝낸다 */
	public void contribute(int amount) {
		requireActive();
		this.currentAmount += amount;
		this.contributionCount += 1;
	}

	/** 정산으로 쌓인 이자를 반영한다 (보통예금처럼 즉시 지급하는 상품은 부르지 않는다) */
	public void accrueInterest(int interest) {
		requireActive();
		this.accruedInterest += interest;
	}

	/** 다음 정산 시각을 옮긴다 */
	public void scheduleNextSettlement(OffsetDateTime next) {
		this.nextSettlementAt = next;
	}

	/**
	 * 만기에 도달했다. 지급은 아이가 수령을 눌러야 일어나므로 여기서는 대기 상태로만 바꾼다.
	 * 정산 대상에서는 빠진다.
	 */
	public void markMatured() {
		requireActive();
		this.status = SavingsSubscriptionStatus.MATURED;
		this.nextSettlementAt = null;
	}

	/**
	 * 만기 수령 완료.
	 */
	public void complete(OffsetDateTime now) {
		if (status != SavingsSubscriptionStatus.ACTIVE && status != SavingsSubscriptionStatus.MATURED) {
			throw new BusinessException(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_ACTIVE,
				"수령할 수 없는 상태입니다. savingsSubscriptionId=" + id + ", status=" + status);
		}
		this.status = SavingsSubscriptionStatus.COMPLETED;
		this.receivedAt = now;
		this.nextSettlementAt = null;
	}

	/**
	 * 중도 해지. 쌓인 이자는 지급하지 않으므로 0으로 지운다 — 남겨 두면 "받지 못한 이자"가
	 * 이력에 남아 지급액과 어긋나 보인다.
	 */
	public void terminate(OffsetDateTime now) {
		requireActive();
		this.status = SavingsSubscriptionStatus.CANCELLED;
		this.accruedInterest = 0;
		this.terminatedAt = now;
		this.receivedAt = now;
		this.nextSettlementAt = null;
	}

	private void requireActive() {
		if (!isActive()) {
			throw new BusinessException(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_ACTIVE,
				"진행 중인 가입이 아닙니다. savingsSubscriptionId=" + id + ", status=" + status);
		}
	}
}
