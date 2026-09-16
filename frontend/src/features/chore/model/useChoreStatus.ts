import { useQuery } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { withStatus, type TodayChoreStatus } from '@/features/chore/lib/choreStatus';
import { choreKeys } from '@/features/chore/model/choreKeys';
import { usePendingCompletionRequest } from '@/features/chore/model/usePendingCompletionRequest';
import type { ChoreDetail, ChoreSummary } from '@/features/chore/model/types';

/** 오늘 (YYYY-MM-DD), 기기 로컬 시간 기준 */
function todayDate(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

/**
 * 할 일 하나의 진행 상태. 상세 화면의 배지가 쓴다.
 *
 * 상세 응답에는 상태가 없다 - 목록과 마찬가지로 대기 중인 요청과 이번 주기 지급 내역을
 * 겹쳐야 나온다. 판정 규칙은 목록과 같은 lib/choreStatus 를 쓴다.
 *
 * 완료 내역은 이 할 일의 주기 시작일부터만 본다. 그 이전 지급은 지난 주기 것이다.
 */
export function useChoreStatus(
  familyId: number | undefined,
  childId: number | undefined,
  chore: ChoreDetail | undefined,
): TodayChoreStatus | undefined {
  const today = todayDate();
  const enabled = familyId != null && childId != null && chore != null;

  const { data: request } = usePendingCompletionRequest(familyId, childId, chore?.choreId);

  const { data: completions } = useQuery({
    queryKey: choreKeys.completions(familyId ?? -1, childId ?? -1, chore?.startDate ?? '', today),
    queryFn: () =>
      choreApi.getCompletions(
        familyId as number,
        childId as number,
        chore?.startDate as string,
        today,
      ),
    enabled,
  });

  if (!enabled || chore == null) return undefined;

  // withStatus 는 목록(ChoreSummary)을 받는다. 상태 판정에 쓰는 필드는 상세에도 모두 있다.
  const summary: ChoreSummary = {
    choreId: chore.choreId,
    childId: chore.childId,
    title: chore.title,
    difficulty: chore.difficulty,
    rewardPoints: chore.rewardPoints,
    repeatType: chore.repeatType,
    startDate: chore.startDate,
    endDate: chore.endDate,
    createdAt: chore.createdAt,
  };

  return withStatus([summary], request ? [request] : [], completions ?? [])[0].status;
}
