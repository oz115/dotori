package com.bana.dotori.wallet.domain;

/**
 * 잔액이 늘었는지 줄었는지를 나타낸다.
 *
 * <p>DB 의 네이티브 ENUM 타입 {@code point_transactions_transaction_type} 과 값이 1:1로 대응한다.
 * 값을 추가하려면 DB 의 ENUM 타입에도 함께 추가해야 한다.
 */
public enum PointTransactionType {

	/** 포인트 증가 (충전·지급·보상·환불) */
	DEPOSIT,

	/** 포인트 감소 (구매·환전·예적금 가입·투자 매수) */
	WITHDRAWAL
}
