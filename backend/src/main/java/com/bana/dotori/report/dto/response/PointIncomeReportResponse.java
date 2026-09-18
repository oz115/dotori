package com.bana.dotori.report.dto.response;

import java.util.List;

/**
 * RPT-02 포인트 수입 통계 조회 응답.
 *
 * <pre>
 * {
 *   "total": 25000,
 *   "byType": [
 *     { "incomeType": "CHORE_REWARD", "amount": 10000, "count": 10 },
 *     { "incomeType": "ALLOWANCE", "amount": 12000, "count": 4 }
 *   ],
 *   "byPeriod": [
 *     { "date": "2026-08-01", "amount": 3000 }
 *   ]
 * }
 * </pre>
 *
 * <p>{@code total} 은 따로 조회하지 않고 {@code byType} 을 더해서 만든다. 쿼리를 하나
 * 줄이는 것보다, <b>{@code total} 과 {@code byType} 의 합이 절대 어긋나지 않는다</b>는 점이
 * 중요하다. 따로 집계하면 두 쿼리 사이에 거래가 들어왔을 때 화면에서 합이 안 맞는다.
 */
public record PointIncomeReportResponse(
	long total,
	List<PointIncomeTypeAmount> byType,
	List<ReportDailyAmount> byPeriod
) {

	public static PointIncomeReportResponse of(List<PointIncomeTypeAmount> byType,
			List<ReportDailyAmount> byPeriod) {

		long total = byType.stream().mapToLong(PointIncomeTypeAmount::amount).sum();
		return new PointIncomeReportResponse(total, byType, byPeriod);
	}

	/** 거래가 없는 경우. 오류가 아니라 0 과 빈 목록으로 응답한다. */
	public static PointIncomeReportResponse empty() {
		return new PointIncomeReportResponse(0L, List.of(), List.of());
	}
}
