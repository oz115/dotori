import { useMutation, useQueryClient } from '@tanstack/react-query';

import { accountApi } from '@/features/account/api/accountApi';
import { accountKeys } from '@/features/account/model/accountKeys';
import type { AccountAuthBody, AccountVerifyBody } from '@/features/account/model/types';

/**
 * 1원 송금 요청 mutation. `POST /api/v1/accounts/auth`.
 * 재호출하면 서버가 새 인증 세션을 만든다 (재전송 겸용).
 */
export function useRequestAccountAuth() {
  return useMutation({
    mutationFn: (body: AccountAuthBody) => accountApi.requestAuth(body),
  });
}

/**
 * 1원 인증 코드 검증 mutation. `POST /api/v1/accounts/auth/verify`.
 * 성공하면 연동 계좌 캐시를 무효화한다.
 */
export function useVerifyAccountAuth() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: AccountVerifyBody) => accountApi.verifyAuth(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
  });
}
