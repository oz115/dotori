package com.bana.dotori.report.dto.response;

import com.bana.dotori.report.dto.ReportTypeAmountRow;
import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * RPT-02 수입 유형별 집계 한 줄.
 *
 * <pre>
 * { "incomeType": "CHORE_REWARD", "amount": 10000, "count": 10 }
 * </pre>
 *
 * <p>{@code incomeType} 은 {@link PointTransactionReason} 상수 이름을 그대로 내보낸다.
 * WLT-06 거래내역의 {@code transactionType} 과 같은 값이라, 클라이언트는 사유 표기를
 * 한 번만 만들면 두 화면에 같이 쓸 수 있다.
 */
public record PointIncomeTypeAmount(
	PointTransactionReason incomeType,
	long amount,
	long count
) {

	public static PointIncomeTypeAmount from(ReportTypeAmountRow row) {
		return new PointIncomeTypeAmount(row.reason(), row.amount(), row.count());
	}
}
