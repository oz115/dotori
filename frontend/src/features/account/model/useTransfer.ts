import { useMutation, useQueryClient } from '@tanstack/react-query';

import { accountApi } from '@/features/account/api/accountApi';
import { accountKeys } from '@/features/account/model/accountKeys';
import type { TransferBody } from '@/features/account/model/types';
import { newIdempotencyKey } from '@/shared/lib/idempotencyKey';

/**
 * 연동 계좌에서 임의 계좌로 이체. 성공하면 내 계좌 캐시를 지워 다음 조회에서
 * 줄어든 잔액을 다시 받아온다 — 금융 mutation 은 낙관적으로 미리 반영하지 않는다.
 */
export function useTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: TransferBody) => accountApi.transfer(body, newIdempotencyKey()),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: accountKeys.me() });
    },
  });
}
