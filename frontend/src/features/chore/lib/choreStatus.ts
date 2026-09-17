import type {
  ChoreCompletionHistoryItem,
  ChoreCompletionSummary,
  ChoreSummary,
} from '@/features/chore/model/types';

/**
 * 할 일에 진행 상태를 얹는다.
 *
 * 서버의 `GET chores` 는 완료 여부를 담지 않는다. 대기 중인 완료 요청과 지급까지 끝난
 * 내역을 따로 받아 choreId 로 겹쳐서 상태를 매긴다 - 셋 다 없으면 TODO 다.
 */

export type TodayChoreStatus = 'TODO' | 'PENDING' | 'DONE';
export type TodayChore = ChoreSummary & { status: TodayChoreStatus };

/** 서버와 같은 기준(KST 자정)으로 본다. 타임존을 안 맞추면 경계가 아홉 시간 밀린다 */
const KST = '+09:00';

/**
 * 이 할 일의 현재 주기가 시작된 순간.
 *
 * 서버의 `Chore.currentPeriodStartedAt()` 과 같은 계산이다 - 크론이 주기가 끝날 때마다
 * startDate 를 앞으로 밀어 주므로, 목록으로 받은 startDate 가 곧 이번 주기의 시작일이다.
 */
export function periodStartedAt(startDate: string): number {
  return Date.parse(`${startDate}T00:00:00${KST}`);
}

/**
 * 완료 내역을 조회할 시작일. 목록에서 가장 이른 주기 시작일이면 모든 할 일을 덮는다.
 *
 * 오늘 하루만 조회하면 안 된다 - 매주·매월 반복은 며칠 전에 승인됐을 수 있고, 그러면
 * 이미 받은 할 일이 아직 안 한 것처럼 보인다.
 */
export function earliestStartDate(chores: ChoreSummary[]): string | undefined {
  return chores.reduce<string | undefined>(
    (earliest, chore) =>
      earliest == null || chore.startDate < earliest ? chore.startDate : earliest,
    undefined,
  );
}

/** 이번 주기에 이미 보상을 받았는지. 지난 주기의 지급은 세지 않는다 */
function isPaidThisPeriod(chore: ChoreSummary, completions: ChoreCompletionHistoryItem[]): boolean {
  const from = periodStartedAt(chore.startDate);
  return completions.some(
    (completion) =>
      completion.choreId === chore.choreId && Date.parse(completion.processedAt) >= from,
  );
}

export function withStatus(
  chores: ChoreSummary[],
  pendingRequests: ChoreCompletionSummary[],
  completions: ChoreCompletionHistoryItem[],
): TodayChore[] {
  const pendingChoreIds = new Set(pendingRequests.map((request) => request.choreId));

  return chores.map((chore) => ({
    ...chore,
    status: isPaidThisPeriod(chore, completions)
      ? 'DONE'
      : pendingChoreIds.has(chore.choreId)
        ? 'PENDING'
        : 'TODO',
  }));
}
