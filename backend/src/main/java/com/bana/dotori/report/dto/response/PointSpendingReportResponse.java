package com.bana.dotori.report.dto.response;

import java.util.List;

/**
 * RPT-03 포인트 소비 통계 조회 응답.
 *
 * <pre>
 * {
 *   "total": 9000,
 *   "byType": [
 *     { "spendingType": "PURCHASE", "amount": 9000, "count": 3 }
 *   ],
 *   "byPeriod": [
 *     { "date": "2026-08-14", "amount": 3000 }
 *   ]
 * }
 * </pre>
 *
 * <p>{@code total} 산출 방식은 {@link PointIncomeReportResponse} 와 같다.
 */
public record PointSpendingReportResponse(
	long total,
	List<PointSpendingTypeAmount> byType,
	List<ReportDailyAmount> byPeriod
) {

	public static PointSpendingReportResponse of(List<PointSpendingTypeAmount> byType,
			List<ReportDailyAmount> byPeriod) {

		long total = byType.stream().mapToLong(PointSpendingTypeAmount::amount).sum();
		return new PointSpendingReportResponse(total, byType, byPeriod);
	}

	/** 거래가 없는 경우. 오류가 아니라 0 과 빈 목록으로 응답한다. */
	public static PointSpendingReportResponse empty() {
		return new PointSpendingReportResponse(0L, List.of(), List.of());
	}
}
