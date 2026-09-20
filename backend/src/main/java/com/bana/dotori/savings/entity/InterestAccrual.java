package com.bana.dotori.savings.entity;

/**
 * 이자가 쌓이는 방식. 상품의 이자율({@code interestRate})을 어떤 기준에 어떻게 곱하는지가
 * 상품마다 달라서, {@link PayoutTiming}(언제 주는가)과 별도로 둔다 — 단리예금과 복리예금은
 * 지급 시점이 똑같이 만기지만 쌓는 방식이 다르다.
 *
 * <p>세 방식 모두 {@code SavingsProductType.targetTotalInterest} 라는 하나의 목표에 도달한다
 * (복리예금은 원안 스펙상 4배 예외). 검산은 {@code SavingsProductTypeTest} 에 있다.
 */
public enum InterestAccrual {

	/**
	 * 매주 (원금 잔액 × 이자율)을 계산해 그 자리에서 지갑으로 지급한다. 보통예금 전용이며,
	 * 이자가 가입 건에 쌓이지 않으므로 {@code accruedInterest} 는 늘 0이다.
	 */
	WEEKLY_PAYOUT,

	/**
	 * 매주 (원금 잔액 × 이자율 ÷ 전체 주수)를 쌓는다. 이자율이 "전체 기간 기준"이라
	 * 주수로 나눠야 만기 총이자가 원금 × 이자율이 된다.
	 *
	 * <p>적립식(소액·중기·장기적금)에서는 매주 원금 잔액이 늘기 때문에, 이 식이 그대로
	 * "평잔(회차별 잔액의 평균) × 이자율"이 된다 — 예: 6회 적립이면
	 * (1+2+…+6)X ÷ 6 × 이자율 = 3.5X × 이자율. 거치식(단리예금)은 잔액이 고정이라
	 * 같은 식이 원금 × 이자율로 떨어진다.
	 */
	WEEKLY_STRAIGHT,

	/**
	 * 매주 ((원금 잔액 + 쌓인 이자) × 이자율)을 쌓는다. 지난주 이자가 이번 주 이자의 기준에
	 * 포함되는 것이 복리다. 이자율은 주당 기준이다.
	 */
	WEEKLY_COMPOUND
}
