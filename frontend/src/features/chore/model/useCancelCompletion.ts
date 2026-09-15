import { useMutation, useQueryClient } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 완료 요청 취소 (아이). 사진을 잘못 올렸을 때 되돌리는 유일한 길이다.
 *
 * 취소해도 같은 주기에 다시 요청할 수 있다 — 서버가 행을 지우지 않고 CANCELLED 로 남기고,
 * 중복을 막는 인덱스는 대기 중인 행만 본다. 다만 사진은 요청에 묶여 있어 다시 찍어야 한다.
 */
export function useCancelCompletion(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (requestId: number) =>
      choreApi.cancelCompletion(familyId as number, childId as number, requestId),
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      queryClient.invalidateQueries({ queryKey: choreKeys.pendingRequests(familyId, childId) });
    },
  });
}
