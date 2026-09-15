import {
  currentYearMonth,
  formatYearMonthRange,
  shiftYearMonth,
  yearMonthDateRange,
} from './purchaseHistoryFilter';

describe('currentYearMonth', () => {
  it('Date를 연-월로 바꾼다 (month는 1~12)', () => {
    expect(currentYearMonth(new Date(2026, 8, 15))).toEqual({ year: 2026, month: 9 });
  });
});

describe('shiftYearMonth', () => {
  it('양수 delta는 미래로 이동한다', () => {
    expect(shiftYearMonth({ year: 2026, month: 8 }, 1)).toEqual({ year: 2026, month: 9 });
  });

  it('음수 delta는 과거로 이동한다', () => {
    expect(shiftYearMonth({ year: 2026, month: 8 }, -1)).toEqual({ year: 2026, month: 7 });
  });

  it('12월에서 다음 달로 가면 연도가 넘어간다', () => {
    expect(shiftYearMonth({ year: 2026, month: 12 }, 1)).toEqual({ year: 2027, month: 1 });
  });

  it('1월에서 이전 달로 가면 전년도 12월이 된다', () => {
    expect(shiftYearMonth({ year: 2026, month: 1 }, -1)).toEqual({ year: 2025, month: 12 });
  });
});

describe('yearMonthDateRange', () => {
  it('해당 월의 1일부터 말일까지를 반환한다', () => {
    expect(yearMonthDateRange({ year: 2026, month: 8 })).toEqual({
      startDate: '2026-08-01',
      endDate: '2026-08-31',
    });
  });

  it('2월(평년)도 말일을 정확히 계산한다', () => {
    expect(yearMonthDateRange({ year: 2026, month: 2 })).toEqual({
      startDate: '2026-02-01',
      endDate: '2026-02-28',
    });
  });
});

describe('formatYearMonthRange', () => {
  it('"YYYY.MM.DD ~ YYYY.MM.DD" 형태로 보여준다', () => {
    expect(formatYearMonthRange({ year: 2026, month: 8 })).toBe('2026.08.01 ~ 2026.08.31');
  });
});
