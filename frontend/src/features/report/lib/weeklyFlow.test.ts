import { buildWeeklyFlow, netFlow } from './weeklyFlow';
import type { ReportPeriod } from '@/features/report/model/types';

/** 2026-09-14(월) ~ 09-20(일). 기준 시각은 KST 9/18(금) 15:00 */
const WEEK: ReportPeriod = { startDate: '2026-09-14', endDate: '2026-09-20' };
const NOW = new Date('2026-09-18T06:00:00Z');

describe('buildWeeklyFlow', () => {
  it('항상 일곱 칸을 만든다 — 축이 월~일로 고정이다', () => {
    const flow = buildWeeklyFlow(WEEK, [], [], NOW);

    expect(flow.days).toHaveLength(7);
    expect(flow.days.map((d) => d.label)).toEqual(['월', '화', '수', '목', '금', '토', '일']);
  });

  // 서버는 거래가 없는 날의 행을 보내지 않는다. 그대로 그리면 막대가 요일과 어긋난다.
  it('서버가 보내지 않은 날을 0 으로 채운다', () => {
    const flow = buildWeeklyFlow(
      WEEK,
      [
        { date: '2026-09-14', amount: 2000 },
        { date: '2026-09-17', amount: 72700 },
      ],
      [{ date: '2026-09-17', amount: 64250 }],
      NOW,
    );

    expect(flow.days.map((d) => d.income)).toEqual([2000, 0, 0, 72700, 0, 0, 0]);
    expect(flow.days.map((d) => d.spending)).toEqual([0, 0, 0, 64250, 0, 0, 0]);
  });

  it('날짜를 요일 자리에 정확히 붙인다', () => {
    const flow = buildWeeklyFlow(WEEK, [{ date: '2026-09-16', amount: 300 }], [], NOW);

    const wednesday = flow.days.find((d) => d.label === '수');
    expect(wednesday?.date).toBe('2026-09-16');
    expect(wednesday?.income).toBe(300);
  });

  // 0 과 "아직 안 옴"을 같게 두면 화면에서 구분할 방법이 없다.
  it('아직 오지 않은 날을 따로 표시한다', () => {
    const flow = buildWeeklyFlow(WEEK, [], [], NOW);

    expect(flow.days.map((d) => d.isFuture)).toEqual([
      false, // 월 9/14
      false, // 화 9/15
      false, // 수 9/16
      false, // 목 9/17
      false, // 금 9/18 — 오늘은 미래가 아니다
      true, // 토 9/19
      true, // 일 9/20
    ]);
  });

  it('지난 주는 일곱 날 모두 미래가 아니다', () => {
    const lastWeek: ReportPeriod = { startDate: '2026-09-07', endDate: '2026-09-13' };
    const flow = buildWeeklyFlow(lastWeek, [], [], NOW);

    expect(flow.days.every((d) => !d.isFuture)).toBe(true);
  });

  // 수입과 소비가 각자 최댓값으로 재면 소비 3천원이 수입 7만원과 같은 높이로 그려진다.
  it('peak 은 수입·소비를 합쳐 가장 큰 하루 금액이다', () => {
    const flow = buildWeeklyFlow(
      WEEK,
      [{ date: '2026-09-14', amount: 2000 }],
      [{ date: '2026-09-16', amount: 64250 }],
      NOW,
    );

    expect(flow.peak).toBe(64250);
  });

  it('거래가 하나도 없으면 peak 이 0 이다 — 화면이 빈 상태로 분기한다', () => {
    expect(buildWeeklyFlow(WEEK, [], [], NOW).peak).toBe(0);
    expect(buildWeeklyFlow(WEEK, undefined, undefined, NOW).peak).toBe(0);
  });

  it('그 주 밖의 날짜는 무시한다', () => {
    const flow = buildWeeklyFlow(WEEK, [{ date: '2026-09-30', amount: 9999 }], [], NOW);

    expect(flow.days.every((d) => d.income === 0)).toBe(true);
    expect(flow.peak).toBe(0);
  });
});

describe('netFlow', () => {
  it('수입에서 소비를 뺀다', () => {
    expect(netFlow(75028, 64993)).toBe(10035);
  });

  it('더 썼으면 음수다', () => {
    expect(netFlow(1000, 3000)).toBe(-2000);
  });
});

/**
 * 실제 서버 응답으로 한 번 더 확인한다. 위 테스트들은 내가 만든 입력을 쓰므로, 서버가
 * 실제로 주는 모양(빠진 날·정렬·필드명)이 달라지면 잡지 못한다.
 *
 * 응답 출처: GET .../families/3/children/5/reports/{income,spending}
 *            ?startDate=2026-09-14&endDate=2026-09-20
 */
describe('buildWeeklyFlow — 실제 서버 응답', () => {
  const INCOME = [
    { date: '2026-09-14', amount: 2000 },
    { date: '2026-09-16', amount: 300 },
    { date: '2026-09-17', amount: 72700 },
    { date: '2026-09-18', amount: 28 },
  ];
  const SPENDING = [
    { date: '2026-09-16', amount: 743 },
    { date: '2026-09-17', amount: 64250 },
    { date: '2026-09-18', amount: 800 },
  ];

  it('빠진 날(화·토·일)을 채워 일곱 칸을 만든다', () => {
    const flow = buildWeeklyFlow(WEEK, INCOME, SPENDING, NOW);

    expect(flow.days.map((d) => d.income)).toEqual([2000, 0, 300, 72700, 28, 0, 0]);
    expect(flow.days.map((d) => d.spending)).toEqual([0, 0, 743, 64250, 800, 0, 0]);
  });

  it('peak 은 목요일 수입이다', () => {
    expect(buildWeeklyFlow(WEEK, INCOME, SPENDING, NOW).peak).toBe(72700);
  });

  it('토·일만 미래다 — 금요일(오늘)은 아니다', () => {
    const flow = buildWeeklyFlow(WEEK, INCOME, SPENDING, NOW);

    expect(flow.days.filter((d) => d.isFuture).map((d) => d.label)).toEqual(['토', '일']);
  });

  it('일자별 합이 응답의 total 과 맞는다', () => {
    const flow = buildWeeklyFlow(WEEK, INCOME, SPENDING, NOW);
    const sum = (pick: (d: (typeof flow.days)[number]) => number) =>
      flow.days.reduce((acc, d) => acc + pick(d), 0);

    expect(sum((d) => d.income)).toBe(75028);
    expect(sum((d) => d.spending)).toBe(65793);
    expect(netFlow(75028, 65793)).toBe(9235);
  });
});
