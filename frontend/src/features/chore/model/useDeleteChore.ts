import { useMutation, useQueryClient } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 집안일 삭제 (부모). 소프트 삭제라 목록에서만 빠지고, 이미 올라온 완료 요청은 계속
 * 처리할 수 있다 — 그래서 대기 중 요청 목록도 함께 무효화한다.
 */
export function useDeleteChore(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (choreId: number) =>
      choreApi.deleteChore(familyId as number, childId as number, choreId),
    onSuccess: (_result, choreId) => {
      if (familyId == null || childId == null) return;
      queryClient.invalidateQueries({ queryKey: choreKeys.list(familyId, childId) });
      queryClient.invalidateQueries({ queryKey: choreKeys.completionRequests(familyId, childId) });
      queryClient.removeQueries({ queryKey: choreKeys.detail(familyId, childId, choreId) });
    },
  });
}
