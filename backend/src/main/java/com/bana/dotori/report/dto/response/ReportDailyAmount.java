package com.bana.dotori.report.dto.response;

import java.time.LocalDate;

/**
 * 일자별 집계 한 줄. 수입(RPT-02)·소비(RPT-03) 응답이 같은 모양을 쓴다.
 *
 * <pre>
 * { "date": "2026-08-01", "amount": 3000 }
 * </pre>
 *
 * <p>거래가 없는 날은 <b>행을 만들지 않는다.</b> 0 으로 채우면 한 달 조회에 늘 31 줄이
 * 나가고, 그래프에서 빈 날을 어떻게 그릴지는 화면이 정할 일이다. 빈 날 채우기는
 * 클라이언트 몫으로 둔다.
 *
 * <p>일자는 <b>KST 기준</b>이다. {@code created_at} 이 TIMESTAMPTZ 라 UTC 로 자르면
 * 한국 시간 자정부터 오전 9 시 사이의 거래가 전날로 집계된다.
 */
public record ReportDailyAmount(
	LocalDate date,
	long amount
) {
}
