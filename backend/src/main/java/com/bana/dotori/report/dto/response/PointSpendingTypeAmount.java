package com.bana.dotori.report.dto.response;

import com.bana.dotori.report.dto.ReportTypeAmountRow;
import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * RPT-03 소비 유형별 집계 한 줄.
 *
 * <pre>
 * { "spendingType": "PURCHASE", "amount": 9000, "count": 3 }
 * </pre>
 *
 * <p>필드 이름만 다르고 내용은 {@link PointIncomeTypeAmount} 와 같다. 명세가 수입·소비의
 * 키를 다르게 정해 두어 타입을 나눴다.
 */
public record PointSpendingTypeAmount(
	PointTransactionReason spendingType,
	long amount,
	long count
) {

	public static PointSpendingTypeAmount from(ReportTypeAmountRow row) {
		return new PointSpendingTypeAmount(row.reason(), row.amount(), row.count());
	}
}
