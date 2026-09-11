import { useQuery } from '@tanstack/react-query';

import { accountApi } from '@/features/account/api/accountApi';
import { accountKeys } from '@/features/account/model/accountKeys';
import { apiErrorCode } from '@/shared/api';

/**
 * 연동 계좌 상세. `GET /api/v1/accounts/me`.
 * 연동이 없으면 서버가 `ACCOUNT_NOT_LINKED` 를 주는데, 이건 정상 상태라 재시도하지 않는다.
 */
export function useMyAccount() {
  return useQuery({
    queryKey: accountKeys.me(),
    queryFn: () => accountApi.getMyAccount(),
    retry: (count, error) => (apiErrorCode(error) === 'ACCOUNT_NOT_LINKED' ? false : count < 2),
    staleTime: 30_000,
  });
}
