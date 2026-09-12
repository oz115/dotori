import { useMutation, useQueryClient } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { newIdempotencyKey } from '@/features/wallet/lib/idempotencyKey';
import { walletKeys } from '@/features/wallet/model/walletKeys';
import type { CashoutBody } from '@/features/wallet/model/types';

/**
 * 보유 포인트 환전 요청. 요청 즉시 포인트가 차감되고(부모 승인 전까지는 PENDING),
 * 그래서 성공 시 잔액 캐시를 지워 다음 조회에서 줄어든 잔액을 다시 받아온다.
 */
export function useCashoutRequest(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: CashoutBody) => {
      if (familyId == null || childId == null) {
        return Promise.reject(new Error('familyId/childId 가 없습니다'));
      }
      return walletApi.requestCashout(familyId, childId, body, newIdempotencyKey());
    },
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      void queryClient.invalidateQueries({ queryKey: walletKeys.balance(familyId, childId) });
    },
  });
}
