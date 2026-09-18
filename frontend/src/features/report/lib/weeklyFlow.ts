import { daysOfWeek, isFutureDate } from '@/features/report/lib/reportPeriod';
import type { ReportDailyAmount, ReportPeriod } from '@/features/report/model/types';

/**
 * 수입·소비의 일자별 집계를 **월~일 7칸**으로 합친다.
 *
 * <p>서버는 거래가 없는 날의 행을 아예 보내지 않는다. 월 단위 화면이라면 그대로 생략하는
 * 것이 맞지만, 주 단위는 축이 일곱 칸으로 고정이라 빈 날을 화면이 채워야 한다 — 안 채우면
 * 막대가 요일과 어긋나 붙는다.
 *
 * <p>아직 오지 않은 날은 0 이 아니라 `isFuture` 로 표시한다. 둘을 같게 두면 "그날 거래가
 * 없었다"와 "아직 안 왔다"가 화면에서 구분되지 않는다.
 */

const WEEKDAY_LABELS = ['월', '화', '수', '목', '금', '토', '일'] as const;

export type WeeklyFlowDay = {
  date: string;
  /** 월~일 라벨 */
  label: string;
  income: number;
  spending: number;
  /** 아직 오지 않은 날 — 막대를 그리지 않는다 */
  isFuture: boolean;
};

export type WeeklyFlow = {
  days: WeeklyFlowDay[];
  /** 막대 높이를 맞출 기준 — 수입·소비 중 가장 큰 하루 금액. 전부 0 이면 0 */
  peak: number;
};

function amountByDate(rows: ReportDailyAmount[] | undefined): Map<string, number> {
  return new Map((rows ?? []).map((row) => [row.date, row.amount]));
}

export function buildWeeklyFlow(
  period: ReportPeriod,
  income: ReportDailyAmount[] | undefined,
  spending: ReportDailyAmount[] | undefined,
  now: Date = new Date(),
): WeeklyFlow {
  const incomeByDate = amountByDate(income);
  const spendingByDate = amountByDate(spending);

  const days = daysOfWeek(period).map((date, index) => ({
    date,
    label: WEEKDAY_LABELS[index],
    income: incomeByDate.get(date) ?? 0,
    spending: spendingByDate.get(date) ?? 0,
    isFuture: isFutureDate(date, now),
  }));

  // 수입과 소비가 같은 기준으로 커야 한다. 각자 최댓값으로 재면 소비 3천원이 수입
  // 7만원과 같은 높이로 그려져 "그날 번 만큼 썼다"로 읽힌다.
  const peak = days.reduce((max, day) => Math.max(max, day.income, day.spending), 0);

  return { days, peak };
}

/** 그 주의 순유입 (수입 총액 − 소비 총액) */
export function netFlow(incomeTotal: number, spendingTotal: number): number {
  return incomeTotal - spendingTotal;
}
