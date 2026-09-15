import { purchaseHistoryDateRange } from './purchaseHistoryFilter';

describe('purchaseHistoryDateRange', () => {
  it('ALL이면 범위가 없다', () => {
    expect(purchaseHistoryDateRange('ALL')).toBeUndefined();
  });

  it('이번 달은 1일부터 오늘까지', () => {
    const now = new Date(2026, 8, 15); // 2026-09-15 (month is 0-indexed)
    expect(purchaseHistoryDateRange('THIS_MONTH', now)).toEqual({
      startDate: '2026-09-01',
      endDate: '2026-09-15',
    });
  });

  it('지난 달은 전월 1일부터 말일까지', () => {
    const now = new Date(2026, 8, 15); // 2026-09-15
    expect(purchaseHistoryDateRange('LAST_MONTH', now)).toEqual({
      startDate: '2026-08-01',
      endDate: '2026-08-31',
    });
  });

  it('1월 기준 지난 달은 전년도 12월', () => {
    const now = new Date(2026, 0, 10); // 2026-01-10
    expect(purchaseHistoryDateRange('LAST_MONTH', now)).toEqual({
      startDate: '2025-12-01',
      endDate: '2025-12-31',
    });
  });
});
