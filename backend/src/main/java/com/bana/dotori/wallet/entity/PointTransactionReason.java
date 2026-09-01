package com.bana.dotori.wallet.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포인트가 움직인 사유.
 *
 * <p>기존 ERD 의 {@code transaction_direction VARCHAR(10)} 컬럼에 문자열로 저장한다.
 * RPT-02(수입 유형별 통계)는 사유를 조건으로 조회할 수 있어야 하므로 사유가 컬럼에 남아야 한다.
 *
 * <p>주의 1: 컬럼명이 실제 의미(reason)와 다르다. 데이터가 쌓이기 전에
 * {@code transaction_direction} → {@code reason} 으로 renaming 하는 것을 팀에 제안한 상태다.
 *
 * <p>주의 2: 컬럼이 VARCHAR(10)이라 상수 이름은 10자를 넘을 수 없다.
 * {@code INVESTMENT} 가 정확히 10자로 상한에 걸려 있다.
 *
 * <p>사유마다 허용되는 방향을 함께 들고 있다. 지급 사유로 차감이 일어나거나
 * 차감 사유로 포인트가 발행되면 통계와 원장이 뒤틀리기 때문에, 방향을 값에 못 박아 둔다.
 *
 * @param depositAllowed  포인트가 늘어나는 거래에 쓸 수 있는가
 * @param withdrawalAllowed 포인트가 줄어드는 거래에 쓸 수 있는가
 */
@Getter
@RequiredArgsConstructor
public enum PointTransactionReason {

	/** WLT-01 아이가 실제 금액을 송금하고 포인트로 환전 */
	CHARGE(true, false),

	/** WLT-02 부모가 실제 송금 없이 포인트를 발행해 지급 */
	GRANT(true, false),

	/** WLT-03 정기 지급 정책에 따른 스케줄러 자동 지급 */
	ALLOWANCE(true, false),

	/** SHOP-06 상점 물품 구매 */
	PURCHASE(false, true),

	/** WLT-07 보유 포인트를 현금으로 환전 */
	CASHOUT(false, true),

	/** SAV 예적금. 가입은 차감, 만기 수령과 중도 해지는 지급이라 양방향이다 */
	SAVINGS(true, true),

	/** INV-T 투자. 매수는 차감, 매도는 지급이라 양방향이다 */
	INVESTMENT(true, true),

	/** QUIZ-03 퀴즈 정답 보상 */
	QUIZ(true, false),

	/** CHORE-09 집안일 완료 보상 */
	CHORE(true, false),

	/** 취소·반려 등으로 되돌려주는 경우 */
	REFUND(true, false);

	private final boolean depositAllowed;
	private final boolean withdrawalAllowed;
}
