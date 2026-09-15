/** 연-월. month 는 1~12 (Date의 0-indexed month와 헷갈리지 않도록). */
export type YearMonth = { year: number; month: number };

export function currentYearMonth(now: Date = new Date()): YearMonth {
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

/** delta만큼 달을 옮긴다 (음수면 과거로). 연도 경계를 자동으로 넘나든다. */
export function shiftYearMonth(yearMonth: YearMonth, delta: number): YearMonth {
  const zeroBasedTotal = yearMonth.year * 12 + (yearMonth.month - 1) + delta;
  return {
    year: Math.floor(zeroBasedTotal / 12),
    month: (((zeroBasedTotal % 12) + 12) % 12) + 1,
  };
}

/** "YYYY-MM-DD" (로컬 날짜, UTC 변환 없이). */
function toDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** 해당 월의 1일 ~ 말일을 API가 받는 날짜 범위로 바꾼다. */
export function yearMonthDateRange(yearMonth: YearMonth): { startDate: string; endDate: string } {
  return {
    startDate: toDateString(new Date(yearMonth.year, yearMonth.month - 1, 1)),
    // new Date(y, m, 0) 은 m월의 "0번째 날" = 전월 말일 → month 그대로 넘기면 이번 달 말일.
    endDate: toDateString(new Date(yearMonth.year, yearMonth.month, 0)),
  };
}

/** 날짜 바에 쓰는 "2026.08.01 ~ 2026.08.31" 형태. */
export function formatYearMonthRange(yearMonth: YearMonth): string {
  const { startDate, endDate } = yearMonthDateRange(yearMonth);
  return `${startDate.replaceAll('-', '.')} ~ ${endDate.replaceAll('-', '.')}`;
}
