import { useQueries, useQuery } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChoreChild } from '@/features/chore/model/useChildrenChores';
import type { ChoreCompletionSummary } from '@/features/chore/model/types';

/**
 * 승인된 완료 요청. 완료 이력에서 "그때 무슨 사진을 보냈는지" 를 보여 주려고 받는다.
 *
 * 완료 이력 목록 자체(`/completions`)는 포인트 원장에서 와서 지급액과 지급 시각만 있고
 * 사진도 아이가 쓴 내용도 없다. 그래서 승인된 요청을 따로 받아 choreId·시각으로 짝지어
 * 붙인다 (choreHistory 의 matchCompletionPhotos).
 *
 * <p>승인된 상태의 이름은 SUCCEED 다 (APPROVED 아님 - 서버 ChoreCompletionStatus).
 *
 * <p>사진 URL 이 몇 분이면 만료되는 presigned 링크라 오래 캐시하지 않는다 (대기함과 같다).
 */
export function useApprovedRequests(familyId: number | undefined, childId: number | undefined) {
  return useQuery({
    queryKey: choreKeys.completionRequests(familyId ?? -1, childId ?? -1, 'SUCCEED'),
    queryFn: () => choreApi.getCompletionRequests(familyId as number, childId as number, 'SUCCEED'),
    enabled: familyId != null && childId != null,
    staleTime: 10_000,
  });
}

/** 응답에 childId 가 없어(childName 만 온다) 어느 아이 것인지 붙여 둔다 */
export type ParentApprovedRequest = ChoreCompletionSummary & { childId: number };

/** 부모 완료 이력용 — 자녀별로 부르고 합친다 (useChildrenPendingRequests 와 같은 구조) */
export function useChildrenApprovedRequests(familyId: number | undefined, children: ChoreChild[]) {
  return useQueries({
    queries: children.map((child) => ({
      queryKey: choreKeys.completionRequests(familyId ?? -1, child.childId, 'SUCCEED'),
      queryFn: () =>
        choreApi.getCompletionRequests(familyId as number, child.childId, 'SUCCEED' as const),
      enabled: familyId != null,
      staleTime: 10_000,
    })),
    combine: (results) => ({
      items: results.flatMap((result, index) =>
        (result.data ?? []).map<ParentApprovedRequest>((request) => ({
          ...request,
          childId: children[index].childId,
        })),
      ),
    }),
  });
}
