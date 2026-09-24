import { useMutation, useQueryClient } from '@tanstack/react-query';

import { choreApi } from '@/features/chore/api/choreApi';
import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 완료 승인 + 보상 지급 (부모). 응답의 rewardPoints 가 이번에 실제로 지급된 금액이다.
 *
 * 대기함과 완료 내역이 함께 바뀌므로 둘 다 무효화한다. 완료 내역은 조회 기간이 키에 들어
 * 있어 기간을 알 수 없으니 접두사로 통째로 턴다.
 *
 * 지갑 잔액은 이 feature 가 모르는 슬라이스라 여기서 안 건드린다 — 호출하는 라우트가
 * 따로 처리한다 (상점 구매와 같은 방식).
 */
export function useApproveCompletion(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (requestId: number) =>
      choreApi.approveCompletion(familyId as number, childId as number, requestId),
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      queryClient.invalidateQueries({ queryKey: choreKeys.completionRequests(familyId, childId) });
      queryClient.invalidateQueries({ queryKey: [...choreKeys.all, 'completions'] });
    },
  });
}
