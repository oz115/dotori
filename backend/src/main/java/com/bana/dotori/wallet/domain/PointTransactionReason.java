package com.bana.dotori.wallet.domain;

/**
 * 포인트가 움직인 사유.
 *
 * <p>기존 ERD 의 {@code transaction_direction VARCHAR(10)} 컬럼에 문자열로 저장한다.
 * {@code transaction_type} 이 이미 입출금 방향을 담고 있어 direction 컬럼이 중복이었고,
 * RPT-02(수입 유형별 통계)는 사유를 조건으로 조회할 수 있어야 하므로 이 컬럼을 사유로 사용한다.
 *
 * <p>주의 1: 컬럼명이 실제 의미(reason)와 다르다. 데이터가 쌓이기 전에
 * {@code transaction_direction} → {@code reason} 으로 renaming 하는 것을 팀에 제안한 상태다.
 *
 * <p>주의 2: 컬럼이 VARCHAR(10)이라 상수 이름은 10자를 넘을 수 없다.
 * {@code INVESTMENT} 가 정확히 10자로 상한에 걸려 있다.
 */
public enum PointTransactionReason {

	/** WLT-01 아이가 실제 금액을 송금하고 포인트로 환전 */
	CHARGE,

	/** WLT-02 부모가 실제 송금 없이 포인트를 발행해 지급 */
	GRANT,

	/** WLT-03 정기 지급 정책에 따른 스케줄러 자동 지급 */
	ALLOWANCE,

	/** SHOP-06 상점 물품 구매 */
	PURCHASE,

	/** WLT-07 보유 포인트를 현금으로 환전 */
	CASHOUT,

	/** SAV 예적금 가입·해지·만기 수령 */
	SAVINGS,

	/** INV-T 투자 매수·매도 */
	INVESTMENT,

	/** QUIZ-03 퀴즈 정답 보상 */
	QUIZ,

	/** CHORE-09 집안일 완료 보상 */
	CHORE,

	/** 취소·반려 등으로 되돌려주는 경우 */
	REFUND
}
