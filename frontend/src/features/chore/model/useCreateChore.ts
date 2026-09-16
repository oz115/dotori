import { useMutation, useQueryClient } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChoreCreateBody } from '@/features/chore/model/types';

/**
 * 집안일 등록 (부모). 성공하면 목록 캐시를 무효화해 새 할 일이 바로 보이게 한다.
 *
 * 응답의 rewardPoints 는 아직 null 이다 — AI 가 비동기로 채우므로 화면은 "보상 산정 중"을
 * 보여 주고, 값이 필요해지는 시점에 다시 조회한다.
 */
export function useCreateChore(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: ChoreCreateBody) =>
      choreApi.createChore(familyId as number, childId as number, body),
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      queryClient.invalidateQueries({ queryKey: choreKeys.list(familyId, childId) });
    },
  });
}
