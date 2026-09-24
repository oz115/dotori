package com.bana.dotori.wallet.entity;

/**
 * 잔액이 늘어나는 거래인지 줄어드는 거래인지 고르기 위한 내부 구분값.
 *
 * <p>DB 에 저장하지 않는다. 원장에는 사유(transaction_type)와 balance_after 만 남고,
 * 증감 방향은 앞 거래의 잔액과 비교하면 알 수 있다. 같은 정보를 두 곳에 두면
 * 어긋날 여지만 생기기 때문에 저장하지 않기로 했다.
 */
public enum PointTransactionType {

	/** 포인트 증가 (충전·지급·보상·환불) */
	DEPOSIT,

	/** 포인트 감소 (구매·환전·예적금 가입·투자 매수) */
	WITHDRAWAL
}
