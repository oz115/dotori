import { useQueries } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChoreChild } from '@/features/chore/model/useChildrenChores';
import type { ChoreCompletionHistoryItem } from '@/features/chore/model/types';

/** 응답의 childName 만으로는 필터를 걸 수 없어(동명이인·이름 변경) childId 를 붙여 둔다 */
export type ParentCompletion = ChoreCompletionHistoryItem & { childId: number };

/**
 * 부모가 보는 가족 전체의 완료 내역. 자녀별로 부르고 합친다 (useChildrenChores 와 같은 이유).
 *
 * 기간을 안 넘기면 서버가 최근 30일로 잡는다.
 *
 * 합친 뒤 최근 순으로 다시 세운다 - 그냥 이어 붙이면 첫째 아이 것이 전부 위에 몰린다.
 */
export function useChildrenCompletions(familyId: number | undefined, children: ChoreChild[]) {
  return useQueries({
    queries: children.map((child) => ({
      queryKey: choreKeys.completions(familyId ?? -1, child.childId, '', ''),
      queryFn: () => choreApi.getCompletions(familyId as number, child.childId),
      enabled: familyId != null,
    })),
    combine: (results) => ({
      items: results
        .flatMap((result, index) =>
          (result.data ?? []).map<ParentCompletion>((completion) => ({
            ...completion,
            childId: children[index].childId,
          })),
        )
        .sort((a, b) => b.processedAt.localeCompare(a.processedAt)),
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
    }),
  });
}
