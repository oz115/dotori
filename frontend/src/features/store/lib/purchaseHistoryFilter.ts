export type PurchaseHistoryFilterKey = 'ALL' | 'THIS_MONTH' | 'LAST_MONTH';

export const PURCHASE_HISTORY_FILTERS: { key: PurchaseHistoryFilterKey; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'THIS_MONTH', label: '이번 달' },
  { key: 'LAST_MONTH', label: '지난 달' },
];

/** "YYYY-MM-DD" (로컬 날짜, UTC 변환 없이). */
function toDateString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * 프리셋 필터를 API 가 받는 날짜 범위로 바꾼다. ALL 이면 undefined — 기간 조건 없이 전체 조회.
 * 새 날짜 선택 라이브러리 없이 자체 계산만으로 처리한다(전체/이번 달/지난 달로 충분해서).
 */
export function purchaseHistoryDateRange(
  filter: PurchaseHistoryFilterKey,
  now: Date = new Date(),
): { startDate: string; endDate: string } | undefined {
  if (filter === 'ALL') return undefined;

  const year = now.getFullYear();
  const month = now.getMonth();

  if (filter === 'THIS_MONTH') {
    return {
      startDate: toDateString(new Date(year, month, 1)),
      endDate: toDateString(now),
    };
  }

  // LAST_MONTH: 지난 달 1일 ~ 마지막 날. new Date(y, m, 0) 은 m월의 "0번째 날" = 전월 말일.
  return {
    startDate: toDateString(new Date(year, month - 1, 1)),
    endDate: toDateString(new Date(year, month, 0)),
  };
}
