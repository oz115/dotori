package com.bana.dotori.wallet.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포인트가 움직인 사유.
 *
 * <p>{@code point_transactions.transaction_type} 컬럼에 저장한다. 이 컬럼은 네이티브 ENUM
 * ({@code point_transactions_transaction_type})이므로 <b>여기 상수 이름이 DDL 의 라벨과
 * 정확히 같아야 한다.</b> 하나라도 어긋나면 그 사유의 거래가 INSERT 단계에서 실패한다.
 * RPT-02(수입 유형별 통계)는 사유를 조건으로 조회할 수 있어야 하므로 사유가 컬럼에 남아야 한다.
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

	/**
	 * SAV 예적금 가입·납입. 이름은 예적금 쪽 기준이고, 지갑에서는 포인트가 빠져나간다.
	 */
	SAVINGS_DEPOSIT(false, true),

	/**
	 * SAV 예적금 만기 수령·중도 해지. 지갑으로 포인트가 들어온다.
	 */
	SAVINGS_WITHDRAWAL(true, false),

	/** INV-T 투자 매수. 지갑에서 포인트가 빠져나간다 */
	INVESTMENT_BUY(false, true),

	/** INV-T 투자 매도. 지갑으로 포인트가 들어온다 */
	INVESTMENT_SELL(true, false),

	/** QUIZ-03 퀴즈 정답 보상 */
	QUIZ_REWARD(true, false),

	/** CHORE-09 집안일 완료 보상 */
	CHORE_REWARD(true, false),

	/** 취소·반려 등으로 되돌려주는 경우 */
	REFUND(true, false);

	private final boolean depositAllowed;
	private final boolean withdrawalAllowed;
}
