import type { ChoreCompletionHistoryItem } from '@/features/chore/model/types';

import { summarizeWeek, weekStart } from './choreHistory';

const done = (processedAt: string, rewardPoints = 100): ChoreCompletionHistoryItem => ({
  choreId: 1,
  choreTitle: '설거지 하기',
  childName: '민준',
  rewardPoints,
  processedAt,
});

describe('weekStart', () => {
  it('주 중간이면 그 주 월요일 0시다', () => {
    // 2026-09-16 은 수요일
    const start = weekStart(new Date(2026, 8, 16, 15, 30));

    expect(start.getDay()).toBe(1);
    expect(start.getDate()).toBe(14);
    expect(start.getHours()).toBe(0);
  });

  it('월요일이면 그날 0시다', () => {
    expect(weekStart(new Date(2026, 8, 14, 9, 0)).getDate()).toBe(14);
  });

  it('일요일은 지난 월요일이 시작이다', () => {
    // getDay() 를 그대로 쓰면 일요일에 이번 주가 통째로 비어 버린다.
    expect(weekStart(new Date(2026, 8, 20, 9, 0)).getDate()).toBe(14);
  });
});

describe('summarizeWeek', () => {
  const now = new Date(2026, 8, 16, 12, 0); // 수요일

  it('이번 주 것만 세고 더한다', () => {
    const summary = summarizeWeek(
      [
        done(new Date(2026, 8, 15, 18, 0).toISOString(), 100),
        done(new Date(2026, 8, 16, 9, 0).toISOString(), 50),
        done(new Date(2026, 8, 13, 9, 0).toISOString(), 500), // 지난 주
      ],
      now,
    );

    expect(summary).toEqual({ count: 2, total: 150 });
  });

  it('비어 있으면 0이다', () => {
    expect(summarizeWeek([], now)).toEqual({ count: 0, total: 0 });
  });
});
