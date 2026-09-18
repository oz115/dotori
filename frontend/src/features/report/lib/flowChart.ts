import type { WeeklyFlowDay } from '@/features/report/lib/weeklyFlow';

/**
 * 주간 흐름 차트의 좌표 계산. SVG 배치는 컴포넌트가 하고 계산은 여기서만 한다 —
 * 투자 차트(`features/investment/lib/chart.ts`)와 같은 구조라 테스트도 같은 방식으로 붙는다.
 */

/** 막대 두께 상한. 칸을 꽉 채우면 두꺼워 보여서 남는 폭은 여백으로 둔다 */
const MAX_BAR_WIDTH = 24;

/** 금액이 0 보다 크면 최소 이만큼은 그린다 — 28원이 아예 안 보이면 "없음"으로 읽힌다 */
const MIN_BAR_HEIGHT = 2;

export type FlowBar = {
  date: string;
  /** 막대 왼쪽 x */
  x: number;
  width: number;
  /** 기준선에서 위로 뻗는 수입 막대 높이 (0 이면 그리지 않는다) */
  incomeHeight: number;
  /** 기준선에서 아래로 뻗는 소비 막대 높이 */
  spendingHeight: number;
  isFuture: boolean;
};

export type FlowChartLayout = {
  bars: FlowBar[];
  /** 기준선 y. 위쪽 절반이 수입, 아래쪽이 소비 */
  baselineY: number;
  height: number;
};

/**
 * 일곱 칸을 균등 분할하고 각 칸 가운데에 막대를 놓는다.
 *
 * <p>수입과 소비는 <b>같은 기준(peak)</b>으로 높이를 잰다. 각자의 최댓값으로 재면
 * 소비 3천원이 수입 7만원과 같은 높이로 그려져 "번 만큼 썼다"로 읽힌다.
 *
 * @param half 기준선에서 위(또는 아래)로 쓸 수 있는 높이
 */
export function layoutFlowChart(
  days: WeeklyFlowDay[],
  peak: number,
  chartWidth: number,
  half: number,
): FlowChartLayout {
  const slot = days.length > 0 ? chartWidth / days.length : 0;
  const width = Math.min(MAX_BAR_WIDTH, Math.max(0, slot - 8));

  const scale = (amount: number) => {
    if (amount <= 0 || peak <= 0) {
      return 0;
    }
    return Math.max(MIN_BAR_HEIGHT, (amount / peak) * half);
  };

  const bars = days.map((day, index) => ({
    date: day.date,
    x: slot * index + (slot - width) / 2,
    width,
    incomeHeight: day.isFuture ? 0 : scale(day.income),
    spendingHeight: day.isFuture ? 0 : scale(day.spending),
    isFuture: day.isFuture,
  }));

  return { bars, baselineY: half, height: half * 2 };
}
