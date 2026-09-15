import { useMemo } from 'react';
import { useQueries } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChoreSummary } from '@/features/chore/model/types';

export type TodayChoreStatus = 'TODO' | 'PENDING' | 'DONE';
export type TodayChore = ChoreSummary & { status: TodayChoreStatus };

/** 오늘 날짜 (YYYY-MM-DD), 기기 로컬 시간 기준 */
function todayDate(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * 자녀 홈의 "오늘 할 일" 목록. `GET chores`(활성 집안일)는 완료 여부를 담지 않아서,
 * 오늘 대기 중인 완료 요청(`completion-requests?status=PENDING`)과 오늘 승인·지급된
 * 내역(`completions`)을 같이 불러와 겹쳐서 상태를 매긴다 — 셋 다 없으면 TODO.
 *
 * familyId·childId 는 다른 슬라이스 값이라 호출하는 쪽(widget)이 넘겨준다.
 */
export function useTodayChores(familyId: number | undefined, childId: number | undefined) {
  const enabled = familyId != null && childId != null;
  const today = todayDate();

  const [choresQuery, pendingQuery, completionsQuery] = useQueries({
    queries: [
      {
        queryKey: choreKeys.list(familyId ?? -1, childId ?? -1),
        queryFn: () => choreApi.getChores(familyId as number, childId as number),
        enabled,
      },
      {
        queryKey: choreKeys.pendingRequests(familyId ?? -1, childId ?? -1),
        queryFn: () =>
          choreApi.getCompletionRequests(familyId as number, childId as number, 'PENDING'),
        enabled,
      },
      {
        queryKey: choreKeys.completions(familyId ?? -1, childId ?? -1, today, today),
        queryFn: () => choreApi.getCompletions(familyId as number, childId as number, today, today),
        enabled,
      },
    ],
  });

  const items = useMemo<TodayChore[]>(() => {
    const chores = choresQuery.data ?? [];
    const pendingChoreIds = new Set((pendingQuery.data ?? []).map((r) => r.choreId));
    const doneChoreIds = new Set((completionsQuery.data ?? []).map((c) => c.choreId));

    return chores.map((chore) => ({
      ...chore,
      status: doneChoreIds.has(chore.choreId)
        ? 'DONE'
        : pendingChoreIds.has(chore.choreId)
          ? 'PENDING'
          : 'TODO',
    }));
  }, [choresQuery.data, pendingQuery.data, completionsQuery.data]);

  return {
    items,
    remainingCount: items.filter((c) => c.status === 'TODO').length,
    isPending:
      !enabled || choresQuery.isPending || pendingQuery.isPending || completionsQuery.isPending,
    isError: choresQuery.isError || pendingQuery.isError || completionsQuery.isError,
  };
}
