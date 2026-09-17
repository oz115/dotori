package com.bana.dotori.savings.entity;

/**
 * 예적금 상품의 납입 방식. 6개 고정 상품이 서로 완전히 다른 방식을 쓰기 때문에
 * 계산 로직·화면 분기 기준으로 둔다.
 */
public enum ContributionStyle {

	/** 보통예금 — 수시입출금 */
	FREE,

	/** 단리·복리예금 — 가입 시 최초 1회 납입 */
	LUMP_SUM,

	/** 소액·장기적금 — 매주 동일 금액 납입 */
	FIXED_INSTALLMENT,

	/** 목표적금 — 목표 금액 안에서 자유롭게 납입 */
	FLEXIBLE_GOAL;

	/**
	 * 가입 시점에 지갑에서 빠져나가는 금액.
	 *
	 * <p>목표적금만 0이다 — 가입 때 정하는 것은 "4주 안에 모을 목표 금액"이지 납입액이 아니라서,
	 * 목표 금액을 그대로 차감하면 모으는 경험 자체가 없어진다. 나머지는 요청 금액이 곧
	 * 최초 납입액이다 (정액 적립식은 이것이 1회차 납입이 된다).
	 *
	 * @param requestedAmount 가입 요청의 amount — 거치식은 원금, 정액 적립식은 회당 납입액,
	 *                        목표적금은 목표 금액
	 */
	public int initialDeposit(int requestedAmount) {
		return this == FLEXIBLE_GOAL ? 0 : requestedAmount;
	}

	/** 가입 이후 추가 납입이 가능한가. 거치식(단리·복리)은 최초 1회로 끝난다 */
	public boolean allowsContribution() {
		return this != LUMP_SUM;
	}

	/** 가입 시 최초 납입이 곧 1회차로 잡히는가 (정액 적립식) */
	public boolean countsInitialDepositAsInstallment() {
		return this == FIXED_INSTALLMENT;
	}
}
