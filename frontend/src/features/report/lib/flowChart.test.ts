import { layoutFlowChart } from './flowChart';
import type { WeeklyFlowDay } from './weeklyFlow';

const LABELS = ['월', '화', '수', '목', '금', '토', '일'];

function week(
  entries: { income?: number; spending?: number; isFuture?: boolean }[],
): WeeklyFlowDay[] {
  return LABELS.map((label, i) => ({
    date: `2026-09-${String(14 + i).padStart(2, '0')}`,
    label,
    income: entries[i]?.income ?? 0,
    spending: entries[i]?.spending ?? 0,
    isFuture: entries[i]?.isFuture ?? false,
  }));
}

const WIDTH = 350;
const HALF = 56;

describe('layoutFlowChart', () => {
  it('일곱 칸을 균등 분할하고 칸 가운데에 막대를 놓는다', () => {
    const { bars } = layoutFlowChart(week([]), 0, WIDTH, HALF);
    const slot = WIDTH / 7;

    expect(bars).toHaveLength(7);
    bars.forEach((bar, i) => {
      // 막대 중심이 칸 중심과 맞아야 요일 라벨과 어긋나지 않는다
      expect(bar.x + bar.width / 2).toBeCloseTo(slot * i + slot / 2, 5);
    });
  });

  it('막대 두께에 상한을 둔다 — 칸을 꽉 채우면 두꺼워 보인다', () => {
    const { bars } = layoutFlowChart(week([]), 0, 1000, HALF);

    expect(bars[0].width).toBe(24);
  });

  it('peak 인 날은 기준선에서 절반 높이를 다 쓴다', () => {
    const days = week([{ income: 1000 }, { spending: 500 }]);
    const { bars } = layoutFlowChart(days, 1000, WIDTH, HALF);

    expect(bars[0].incomeHeight).toBe(HALF);
    expect(bars[1].spendingHeight).toBe(HALF / 2);
  });

  it('수입과 소비를 같은 기준으로 잰다', () => {
    // 같은 금액이면 위아래 높이가 같아야 한다 — 각자 최댓값으로 재면 어긋난다
    const days = week([{ income: 500, spending: 500 }]);
    const { bars } = layoutFlowChart(days, 1000, WIDTH, HALF);

    expect(bars[0].incomeHeight).toBe(bars[0].spendingHeight);
  });

  // 28원이 0px 로 그려지면 "그날 거래 없음"으로 읽힌다.
  it('0 보다 큰 금액은 최소 2px 은 그린다', () => {
    const days = week([{ income: 28 }]);
    const { bars } = layoutFlowChart(days, 72700, WIDTH, HALF);

    expect(bars[0].incomeHeight).toBe(2);
  });

  it('0 인 날은 막대를 그리지 않는다', () => {
    const { bars } = layoutFlowChart(week([]), 1000, WIDTH, HALF);

    expect(bars.every((bar) => bar.incomeHeight === 0 && bar.spendingHeight === 0)).toBe(true);
  });

  it('아직 오지 않은 날은 막대를 그리지 않는다', () => {
    const days = week([{}, {}, {}, {}, {}, { income: 999, isFuture: true }]);
    const { bars } = layoutFlowChart(days, 999, WIDTH, HALF);

    expect(bars[5].isFuture).toBe(true);
    expect(bars[5].incomeHeight).toBe(0);
  });

  it('peak 이 0 이면 나눗셈을 하지 않는다', () => {
    const { bars } = layoutFlowChart(week([{ income: 100 }]), 0, WIDTH, HALF);

    expect(bars[0].incomeHeight).toBe(0);
    expect(Number.isNaN(bars[0].incomeHeight)).toBe(false);
  });

  // 폭을 재기 전(onLayout 이전) 첫 렌더에서 0 이 들어온다.
  it('폭이 0 이어도 터지지 않는다', () => {
    const { bars } = layoutFlowChart(week([{ income: 100 }]), 100, 0, HALF);

    expect(bars).toHaveLength(7);
    expect(bars[0].width).toBe(0);
  });

  it('기준선은 절반 높이에 있고 전체 높이는 그 두 배다', () => {
    const layout = layoutFlowChart(week([]), 0, WIDTH, HALF);

    expect(layout.baselineY).toBe(HALF);
    expect(layout.height).toBe(HALF * 2);
  });
});
