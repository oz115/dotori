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

	/** 소액·중기·장기적금 — 매주 동일 금액 납입 */
	FIXED_INSTALLMENT;

	/** 가입 이후 추가 납입이 가능한가. 거치식(단리·복리)은 최초 1회로 끝난다 */
	public boolean allowsContribution() {
		return this != LUMP_SUM;
	}

	/** 가입 시 최초 납입이 곧 1회차로 잡히는가 (정액 적립식) */
	public boolean countsInitialDepositAsInstallment() {
		return this == FIXED_INSTALLMENT;
	}
}
