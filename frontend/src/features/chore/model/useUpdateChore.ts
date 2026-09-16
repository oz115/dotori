import { useMutation, useQueryClient } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';
import type { ChoreUpdateBody } from '@/features/chore/model/types';

/** 집안일 수정 (부모). 목록과 상세 캐시를 둘 다 무효화한다. */
export function useUpdateChore(
  familyId: number | undefined,
  childId: number | undefined,
  choreId: number | undefined,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: ChoreUpdateBody) =>
      choreApi.updateChore(familyId as number, childId as number, choreId as number, body),
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      queryClient.invalidateQueries({ queryKey: choreKeys.list(familyId, childId) });
      if (choreId != null) {
        queryClient.invalidateQueries({ queryKey: choreKeys.detail(familyId, childId, choreId) });
      }
    },
  });
}
