package com.bana.dotori.report.dto;

import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * 사유별 집계 결과 한 줄 (RPT-02·03 내부용).
 *
 * <p>JPQL 생성자 표현식이 직접 채운다. {@code sum}·{@code count} 가 {@code Long} 을
 * 돌려주므로 필드도 {@code Long} 이어야 한다 — {@code long} 으로 두면 Hibernate 가
 * 맞는 생성자를 못 찾아 조회 자체가 실패한다.
 *
 * <p>응답 타입({@code PointIncomeTypeAmount}·{@code PointSpendingTypeAmount})과 나눠 둔
 * 이유는 수입·소비가 같은 모양을 쓰면서도 명세상 필드 이름이 다르기 때문이다
 * ({@code incomeType} / {@code spendingType}). 집계는 한 번만 짜고 이름만 갈라 붙인다.
 */
public record ReportTypeAmountRow(
	PointTransactionReason reason,
	Long amount,
	Long count
) {
}
