import { useMutation, useQueryClient } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { walletKeys } from '@/features/wallet/model/walletKeys';
import { newIdempotencyKey } from '@/shared/lib/idempotencyKey';
import type { ChargeBody } from '@/features/wallet/model/types';

/**
 * 실제 계좌에서 송금해 포인트로 충전. 성공하면 잔액 캐시를 지워 다음 조회에서
 * 서버 값을 다시 받아온다 — 금융 mutation 은 낙관적으로 미리 반영하지 않는다.
 */
export function useChargeWallet(familyId: number | undefined, childId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: ChargeBody) => {
      if (familyId == null || childId == null) {
        return Promise.reject(new Error('familyId/childId 가 없습니다'));
      }
      return walletApi.charge(familyId, childId, body, newIdempotencyKey());
    },
    onSuccess: () => {
      if (familyId == null || childId == null) return;
      void queryClient.invalidateQueries({ queryKey: walletKeys.balance(familyId, childId) });
    },
  });
}
