package com.bana.dotori.savings.entity;

/**
 * 예적금 가입 건의 상태.
 */
public enum SavingsSubscriptionStatus {

	/** 정상 진행 중 */
	ACTIVE,

	/** 만기 도달, 지급 대기 */
	MATURED,

	/** 중도 해지 */
	CANCELLED,

	/** 수령 완료 */
	COMPLETED
}
