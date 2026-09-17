import type {
  ChoreCompletionHistoryItem,
  ChoreCompletionSummary,
  ChoreSummary,
} from '@/features/chore/model/types';

/**
 * 할 일에 진행 상태를 얹는다.
 *
 * 서버의 `GET chores` 는 완료 여부를 담지 않는다. 대기 중인 완료 요청, 거절된 요청,
 * 지급까지 끝난 내역을 따로 받아 choreId 로 겹쳐서 상태를 매긴다 - 넷 다 없으면 TODO 다.
 */

export type TodayChoreStatus = 'TODO' | 'PENDING' | 'REJECTED' | 'DONE';
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

/**
 * 이번 주기에 거절당한 적이 있는지.
 *
 * 지난 주기의 거절은 세지 않는다 - 크론이 주기를 밀면 그 할 일은 새로 시작하는 것이라,
 * 지난주에 퇴짜 맞은 흔적이 이번 주 화면에 남아 있으면 안 된다.
 */
function isRejectedThisPeriod(
  chore: ChoreSummary,
  rejectedRequests: ChoreCompletionSummary[],
): boolean {
  const from = periodStartedAt(chore.startDate);
  return rejectedRequests.some(
    (request) => request.choreId === chore.choreId && Date.parse(request.requestedAt) >= from,
  );
}

/**
 * 상태를 겹친다. 우선순위는 DONE > PENDING > REJECTED > TODO 다.
 *
 * PENDING 이 REJECTED 를 이기는 것이 중요하다 - 거절당한 뒤 아이가 다시 보냈으면 지금은
 * "확인 중" 이지 "거절됨" 이 아니다. 거절 기록은 그대로 남아 있으므로 순서를 뒤집으면
 * 새로 보낸 요청이 화면에서 사라진다.
 */
export function withStatus(
  chores: ChoreSummary[],
  pendingRequests: ChoreCompletionSummary[],
  completions: ChoreCompletionHistoryItem[],
  rejectedRequests: ChoreCompletionSummary[] = [],
): TodayChore[] {
  const pendingChoreIds = new Set(pendingRequests.map((request) => request.choreId));

  return chores.map((chore) => ({
    ...chore,
    status: isPaidThisPeriod(chore, completions)
      ? 'DONE'
      : pendingChoreIds.has(chore.choreId)
        ? 'PENDING'
        : isRejectedThisPeriod(chore, rejectedRequests)
          ? 'REJECTED'
          : 'TODO',
  }));
}
