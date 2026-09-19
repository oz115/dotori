import { useMutation, useQueryClient } from '@tanstack/react-query';

import { accountApi } from '@/features/account/api/accountApi';
import { accountKeys } from '@/features/account/model/accountKeys';

/**
 * 계좌 연동 해제 mutation. `DELETE /api/v1/accounts/me`.
 * 성공 시 연동 계좌 관련 전체 캐시를 무효화한다.
 */
export function useUnlinkAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => accountApi.unlink(),
    onSuccess: () => {
      queryClient.setQueryData(accountKeys.me(), null);
      queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
  });
}
