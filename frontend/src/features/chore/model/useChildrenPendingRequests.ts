import { useQueries } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChoreChild } from '@/features/chore/model/useChildrenChores';
import type { ChoreCompletionSummary } from '@/features/chore/model/types';

/** 응답에 childId 가 없어서(childName 만 온다) 어느 아이 경로로 처리할지 함께 들고 다닌다 */
export type ParentCompletionRequest = ChoreCompletionSummary & { childId: number };

/**
 * 부모 대기함. 자녀별로 부르고 합친다 (useChildrenChores 와 같은 이유).
 *
 * 승인·거절 API 도 경로에 childId 가 필요한데 목록 응답에는 이름만 있다. 나중에 되짚을
 * 방법이 없으므로 합칠 때 childId 를 붙여 둔다.
 *
 * 사진 URL 이 몇 분이면 만료되는 presigned 링크라 오래 캐시하지 않는다.
 */
export function useChildrenPendingRequests(familyId: number | undefined, children: ChoreChild[]) {
  return useQueries({
    queries: children.map((child) => ({
      queryKey: choreKeys.completionRequests(familyId ?? -1, child.childId, 'PENDING'),
      queryFn: () =>
        choreApi.getCompletionRequests(familyId as number, child.childId, 'PENDING' as const),
      enabled: familyId != null,
      staleTime: 10_000,
    })),
    combine: (results) => ({
      items: results
        .flatMap((result, index) =>
          (result.data ?? []).map<ParentCompletionRequest>((request) => ({
            ...request,
            childId: children[index].childId,
          })),
        )
        // 오래 기다린 요청이 위로 온다 - 부모는 밀린 것부터 처리한다.
        .sort((a, b) => a.requestedAt.localeCompare(b.requestedAt)),
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
    }),
  });
}
