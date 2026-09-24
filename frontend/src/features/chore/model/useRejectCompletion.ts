import { useMutation, useQueryClient } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 완료 거절 (부모). 사유는 보내지 않는다 — 서버에 담을 자리가 없다.
 *
 * 거절해도 아이는 같은 주기에 다시 요청할 수 있다. 막는 것은 지급이지 요청이 아니다.
 * 지급이 없으므로 완료 내역은 건드리지 않고 대기함만 무효화한다.
 */
export function useRejectCompletion(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (requestId: number) =>
      choreApi.rejectCompletion(familyId as number, childId as number, requestId),
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      queryClient.invalidateQueries({ queryKey: choreKeys.completionRequests(familyId, childId) });
    },
  });
}
