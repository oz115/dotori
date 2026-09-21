package com.bana.dotori.savings.entity;

import com.bana.dotori.family.dto.FamilySettings;

import lombok.Getter;

/**
 * 6개 고정 예·적금 상품의 스펙. 부모가 새로 만들거나 고치지 않는 카탈로그라 DB 대신
 * 코드에 고정한다 (DB {@code savings_products} 행은 이 상수들을 그대로 반영한 seed 데이터일 뿐이다).
 *
 * <p>가족마다 달라지는 값은 "최대 납입/예치 가능 금액(X)" 하나뿐이고, 이는 가족의
 * {@code weeklyRewardLimit}(RL, 주당 예적금·투자·퀴즈 보상 한도)에서 {@link #calculateMaxAmount}
 * 로 매번 계산한다 — 정적 카탈로그에 저장하지 않는다. 가입 시점에 계산한 값을
 * {@code savings_subscriptions.principal} 에 스냅샷해서, 이후 가족 설정이 바뀌어도
 * 이미 가입된 건은 영향받지 않게 한다.
 *
 * <p><b>공통 계산식</b>: 이 상품이 목표로 하는 만기 총이자는 기본적으로
 * {@code REWARD_COEFFICIENT(0.4) × rewardRatio × RL} 이다 ({@link #targetTotalInterest}).
 * 단, 실제로 그 목표를 맞추는 {@code calculateMaxAmount} 공식은 상품의 이자 계산 방식(단리/복리/
 * 정액적립)에 따라 다르다. <b>복리예금은 원안 그대로 목표의 4배(0.64·RL)를 내도록 의도적으로
 * 크게 잡혀 있다</b> — 검산해보면 같은 rewardRatio(0.4)인 단리예금과 4배 차이가 나지만, 이는
 * 원안 스펙을 그대로 반영한 것이며 {@code targetTotalInterest} 불변식 검증 대상에서 제외한다.
 * 나머지 5개는 {@link InterestAccrual} 의 방식대로 계산하면 모두 목표 총이자에 정확히 도달한다
 * ({@code SavingsProductTypeTest} 가 상품마다 검산한다).
 */
@Getter
public enum SavingsProductType {

	/** 자유 입출금, 무기한. 매주 정산일에 현재 잔액 기준 이자를 즉시 지급한다 */
	ORDINARY(1, 0.1, null, ContributionStyle.FREE, PayoutTiming.WEEKLY_IMMEDIATE, InterestAccrual.WEEKLY_PAYOUT, null,
		"자유로운 대신 이자가 적다는 점 학습",
		"현재 금액을 다음 주까지 유지하면 이자를 받아요. 예: 500p를 유지하면 5p.",
		rewardLimit -> rewardLimit * 4L),

	/** 가입 시 최초 1회 납입, 4주 만기. 이자는 매주 계산만 하고 만기에 원금과 함께 지급 */
	SIMPLE_INTEREST(15, 0.4, 4, ContributionStyle.LUMP_SUM, PayoutTiming.MATURITY_LUMP_SUM, InterestAccrual.WEEKLY_STRAIGHT, null,
		"원금에만 이자가 붙는 단리 학습",
		"최초 납입한 원금에 매주 이자가 붙어요. 4주 후 원금과 총이자를 함께 받아요.",
		rewardLimit -> Math.round(rewardLimit * 16.0 / 15.0)),

	/** 가입 시 최초 1회 납입, 4주 만기. 이자를 매주 원금에 반영하고 만기에 함께 지급 */
	COMPOUND_INTEREST(10, 0.4, 4, ContributionStyle.LUMP_SUM, PayoutTiming.MATURITY_LUMP_SUM, InterestAccrual.WEEKLY_COMPOUND, null,
		"이자에도 다시 이자가 붙는 복리 학습",
		"매주 이자가 원금에 더해져 다음 주 이자도 커져요. 동일 금액의 단리와 비교해보세요.",
		// 원안 표의 공식 그대로: 만기 총이자가 항상 0.64·RL(같은 rewardRatio 의 단리보다 4배)이 되도록
		// 의도적으로 잡은 값. targetTotalInterest(0.16·RL) 와는 다르다 — 이 상품만의 예외.
		rewardLimit -> Math.round(rewardLimit * 0.64 / (Math.pow(1.1, 4) - 1))),

	/** 가입 시 매주 납입액을 정하고 그대로 2회 납입, 2회 완료 시 만기 */
	SMALL_INSTALLMENT(10, 0.5, 2, ContributionStyle.FIXED_INSTALLMENT, PayoutTiming.MATURITY_LUMP_SUM, InterestAccrual.WEEKLY_STRAIGHT, 2,
		"적은 금액으로 저축을 완주하는 경험",
		"매주 같은 금액을 2번 납입하면 원금과 만기 이자를 함께 받아요.",
		// TODO(SAV): 적립식 이자 계산 방식(평잔? 회차별 잔여기간?) 미정 — 원안 표의 공식을 그대로 두었다.
		// targetTotalInterest 와의 일치를 아직 검증하지 못했다.
		rewardLimit -> Math.round(rewardLimit * 4.0 / 3.0)),

	/** 가입 시 매주 납입액을 정하고 그대로 4회 납입, 4회 완료 시 만기 */
	MID_TERM_INSTALLMENT(15, 0.5, 4, ContributionStyle.FIXED_INSTALLMENT, PayoutTiming.MATURITY_LUMP_SUM, InterestAccrual.WEEKLY_STRAIGHT, 4,
		"기간이 길수록 이자율이 유리해지는 경험",
		"매주 같은 금액을 4번 납입하면 원금과 만기 이자를 함께 받아요.",
		// 정액 적립식 총이자 = X × (n+1)/2 × 이자율 → 2.5X × 15% = 0.2·RL 이므로 X = 8/15·RL
		rewardLimit -> Math.round(rewardLimit * 8.0 / 15.0)),

	/** 가입 시 매주 납입액을 정하고 그대로 6회 납입, 6회 완료 시 만기 */
	LONG_TERM_INSTALLMENT(20, 0.5, 6, ContributionStyle.FIXED_INSTALLMENT, PayoutTiming.MATURITY_LUMP_SUM, InterestAccrual.WEEKLY_STRAIGHT, 6,
		"정해진 약속을 오랫동안 지키는 경험",
		"매주 같은 금액을 6번 납입하면 원금과 만기 이자를 함께 받아요.",
		// TODO(SAV): 위와 동일.
		rewardLimit -> Math.round(rewardLimit * 2.0 / 7.0));

	/** 모든 상품에 공통으로 적용되는 보상 배분 계수. rewardRatio 와 곱해 목표 총이자를 정한다 */
	private static final double REWARD_COEFFICIENT = 0.4;

	/**
	 * 모든 상품 공통 최소 가입 금액(도토리). 의미 없이 작은 금액으로 가입하는 것을 막는 하한이다.
	 * 상한(X)은 가족 설정(RL)에 따라 달라지지만, 하한은 카탈로그 공통값으로 고정한다.
	 */
	private static final long MIN_AMOUNT = 100L;

	/** 연 단위가 아니라 이 상품의 정산 주기(1주 또는 전체 기간) 기준 이자율(%) */
	private final int interestRate;

	/** 상품별 보상 배분 비율 (0.1/0.4/0.5) */
	private final double rewardRatio;

	/** 가입부터 만기까지 기간(주). 보통예금은 무기한이라 null */
	private final Integer termWeeks;

	private final ContributionStyle contributionStyle;

	private final PayoutTiming payoutTiming;

	private final InterestAccrual interestAccrual;

	/** 정액 적립식 상품의 총 납입 회차. 해당 없으면 null */
	private final Integer installmentCount;

	private final String educationGoal;

	private final String preSignupMessage;

	private final MaxAmountFormula maxAmountFormula;

	SavingsProductType(int interestRate, double rewardRatio, Integer termWeeks, ContributionStyle contributionStyle,
		PayoutTiming payoutTiming, InterestAccrual interestAccrual, Integer installmentCount, String educationGoal,
		String preSignupMessage, MaxAmountFormula maxAmountFormula) {
		this.interestRate = interestRate;
		this.rewardRatio = rewardRatio;
		this.termWeeks = termWeeks;
		this.contributionStyle = contributionStyle;
		this.payoutTiming = payoutTiming;
		this.interestAccrual = interestAccrual;
		this.installmentCount = installmentCount;
		this.educationGoal = educationGoal;
		this.preSignupMessage = preSignupMessage;
		this.maxAmountFormula = maxAmountFormula;
	}

	/**
	 * 가족의 온보딩 설정(RL)에서 이 상품의 최대 납입/예치 가능 금액(X)을 계산한다.
	 * 거치식(단리·복리)은 최초 납입 원금의 상한, 정액 적립식(소액·중기·장기)은 회당 납입액의 상한,
	 * 보통예금은 보유 잔액 상한을 의미한다.
	 */
	public long calculateMaxAmount(FamilySettings familySettings) {
		return maxAmountFormula.calculate(familySettings.weeklyRewardLimit());
	}

	/** 이 상품의 최소 가입 금액(도토리). 지금은 모든 상품이 같은 하한을 쓴다. */
	public long getMinAmount() {
		return MIN_AMOUNT;
	}

	/**
	 * 이 상품이 만기까지 지급하도록 설계된 목표 총이자. {@code calculateMaxAmount} 로 나온 한도로
	 * 실제 이자를 계산했을 때 이 값과 같아야 "같은 보상 비율" 이라는 설계가 지켜진 것이다.
	 * ({@link #COMPOUND_INTEREST} 는 원안 스펙상 예외로, 이 값의 4배를 지급한다.)
	 */
	public long targetTotalInterest(long weeklyRewardLimit) {
		return Math.round(REWARD_COEFFICIENT * rewardRatio * weeklyRewardLimit);
	}

	/**
	 * 이번 주 정산에서 쌓(거나 지급할) 이자. 방식별 계산 근거는 {@link InterestAccrual} 참고.
	 *
	 * @param principalBalance 지금까지 납입된 원금 누계 (이자 제외)
	 * @param accruedInterest  지금까지 쌓인 이자 누계. 복리만 이 값을 기준에 포함한다
	 */
	public long weeklyInterest(long principalBalance, long accruedInterest) {
		return switch (interestAccrual) {
			case WEEKLY_PAYOUT -> Math.round(principalBalance * interestRate / 100.0);
			case WEEKLY_STRAIGHT -> Math.round(principalBalance * interestRate / 100.0 / termWeeks);
			case WEEKLY_COMPOUND -> Math.round((principalBalance + accruedInterest) * interestRate / 100.0);
		};
	}

	/** 이자를 쌓아 두지 않고 정산 즉시 지갑으로 보내는 상품인가 (보통예금) */
	public boolean paysInterestImmediately() {
		return interestAccrual == InterestAccrual.WEEKLY_PAYOUT;
	}

	@FunctionalInterface
	private interface MaxAmountFormula {
		long calculate(long weeklyRewardLimit);
	}
}
