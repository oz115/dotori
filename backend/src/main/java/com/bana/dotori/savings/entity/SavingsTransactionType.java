package com.bana.dotori.savings.entity;

/**
 * 예적금 가입 건 안에서 일어난 거래의 종류.
 *
 * <p>{@code savings_transactions.transaction_type} 컬럼에 저장한다. 이 컬럼은 네이티브 ENUM
 * ({@code savings_transactions_transaction_type})이므로 <b>여기 상수 이름이 DDL 라벨과 정확히
 * 같아야 한다.</b>
 *
 * <p>지갑의 {@code point_transactions} 와 역할이 다르다. 저쪽은 "아이 지갑의 잔액이 어떻게
 * 변했는가"의 원장이고, 이쪽은 "이 가입 건 안에서 무슨 일이 있었는가"의 이력이다. 이자 적립처럼
 * 지갑이 전혀 움직이지 않는 사건도 있어야 만기 지급액을 설명할 수 있다.
 */
public enum SavingsTransactionType {

	/** 가입 시 최초 납입 */
	INITIAL_DEPOSIT,

	/** 가입 이후 추가 납입 */
	CONTRIBUTION,

	/** 주간 정산으로 발생한 이자. 보통예금은 곧바로 지갑으로 나가고, 나머지는 가입 건에 쌓인다 */
	INTEREST,

	/** 중도 해지 반환금 */
	EARLY_TERMINATION_PAYOUT,

	/** 만기 수령액 */
	MATURITY_PAYOUT
}
