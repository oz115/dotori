import type { ChoreCompletionHistoryItem } from '@/features/chore/model/types';

/**
 * 완료 내역 화면의 계산. 서버는 기간 안의 지급 기록만 주므로 "이번 주"는 여기서 가른다.
 */

/**
 * 이번 주가 시작된 순간 (월요일 0시, 기기 로컬 시간).
 *
 * 일요일을 한 주의 시작으로 보는 `getDay()`(0=일)를 그대로 쓰면 일요일에 이번 주가
 * 비어 버린다. 월요일 기준으로 옮겨서 센다.
 */
export function weekStart(now: Date): Date {
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  const daysSinceMonday = (start.getDay() + 6) % 7;
  start.setDate(start.getDate() - daysSinceMonday);
  return start;
}

/** 이번 주에 끝낸 할 일 건수와 받은 도토리 합계 */
export function summarizeWeek(
  completions: ChoreCompletionHistoryItem[],
  now: Date,
): { count: number; total: number } {
  const from = weekStart(now).getTime();

  return completions.reduce(
    (acc, completion) =>
      Date.parse(completion.processedAt) >= from
        ? { count: acc.count + 1, total: acc.total + completion.rewardPoints }
        : acc,
    { count: 0, total: 0 },
  );
}
