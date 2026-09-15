import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { earliestStartDate, withStatus, type TodayChore } from '@/features/chore/lib/choreStatus';
import { choreKeys } from '@/features/chore/model/choreKeys';

export type { TodayChore, TodayChoreStatus } from '@/features/chore/lib/choreStatus';

/** 오늘 날짜 (YYYY-MM-DD), 기기 로컬 시간 기준 */
function todayDate(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * 진행 상태가 얹힌 활성 할 일 목록. 자녀 홈의 "오늘 할 일"과 할 일 탭이 같이 쓴다.
 *
 * 세 번 조회한다 - 할 일 목록에는 완료 여부가 없어서, 대기 중인 완료 요청과 지급이 끝난
 * 내역을 겹쳐야 상태가 나온다. 겹치는 규칙은 lib/choreStatus 에 있다.
 *
 * 완료 내역의 조회 기간은 **목록에서 가장 이른 주기 시작일부터**다. 오늘 하루만 물으면
 * 매주·매월 반복이 며칠 전에 승인된 경우가 빠져서, 이미 받은 할 일이 다시 TODO 로 보인다.
 * 그래서 할 일 목록을 받은 뒤에야 기간이 정해지고, 완료 내역 조회가 그 뒤를 따른다.
 *
 * familyId·childId 는 다른 슬라이스 값이라 호출하는 쪽이 넘겨준다.
 */
export function useTodayChores(familyId: number | undefined, childId: number | undefined) {
  const enabled = familyId != null && childId != null;

  const choresQuery = useQuery({
    queryKey: choreKeys.list(familyId ?? -1, childId ?? -1),
    queryFn: () => choreApi.getChores(familyId as number, childId as number),
    enabled,
  });

  const pendingQuery = useQuery({
    queryKey: choreKeys.pendingRequests(familyId ?? -1, childId ?? -1),
    queryFn: () => choreApi.getCompletionRequests(familyId as number, childId as number, 'PENDING'),
    enabled,
    // 사진 URL 이 몇 분이면 만료되는 presigned 링크라 오래 캐시하지 않는다.
    staleTime: 10_000,
  });

  const chores = useMemo(() => choresQuery.data ?? [], [choresQuery.data]);
  const today = todayDate();
  const from = earliestStartDate(chores) ?? today;
  const needsCompletions = enabled && chores.length > 0;

  const completionsQuery = useQuery({
    queryKey: choreKeys.completions(familyId ?? -1, childId ?? -1, from, today),
    queryFn: () => choreApi.getCompletions(familyId as number, childId as number, from, today),
    enabled: needsCompletions,
  });

  const items = useMemo<TodayChore[]>(
    () => withStatus(chores, pendingQuery.data ?? [], completionsQuery.data ?? []),
    [chores, pendingQuery.data, completionsQuery.data],
  );

  return {
    items,
    remainingCount: items.filter((chore) => chore.status === 'TODO').length,
    isPending:
      !enabled ||
      choresQuery.isPending ||
      pendingQuery.isPending ||
      // 할 일이 없으면 완료 내역을 묻지 않는다. 그 경우 이 쿼리는 영영 pending 이므로 뺀다.
      (needsCompletions && completionsQuery.isPending),
    isError: choresQuery.isError || pendingQuery.isError || completionsQuery.isError,
  };
}
