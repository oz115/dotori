import { useMutation, useQueryClient } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { walletKeys } from '@/features/wallet/model/walletKeys';
import { newIdempotencyKey } from '@/shared/lib/idempotencyKey';

/**
 * 환전 거절 (부모). 요청 때 차감했던 포인트를 서버가 자녀에게 환급하므로 목록과 함께
 * 잔액 캐시도 턴다. 송금은 일어나지 않아 부모 계좌는 그대로다.
 */
export function useDeclineCashout(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (cashoutRequestId: number) => {
      if (familyId == null || childId == null) {
        return Promise.reject(new Error('familyId/childId 가 없습니다'));
      }
      return walletApi.declineCashout(familyId, childId, cashoutRequestId, newIdempotencyKey());
    },
    onSettled: () => {
      if (familyId == null || childId == null) return;
      void queryClient.invalidateQueries({
        queryKey: walletKeys.cashoutRequestsOf(familyId, childId),
      });
      void queryClient.invalidateQueries({ queryKey: walletKeys.balance(familyId, childId) });
    },
  });
}
