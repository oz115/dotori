import { useQuery } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { walletKeys } from '@/features/wallet/model/walletKeys';
import type { CashoutStatus } from '@/features/wallet/model/types';

/**
 * 환전 요청 목록. 부모 홈은 승인 대기(PENDING) 건수를 세는 데 쓴다.
 */
export function useCashoutRequests(
  familyId: number | undefined,
  childId: number | undefined,
  status: CashoutStatus = 'PENDING',
) {
  return useQuery({
    queryKey: walletKeys.cashoutRequests(familyId ?? -1, childId ?? -1, status),
    queryFn: () => walletApi.getCashoutRequests(familyId as number, childId as number, status),
    enabled: familyId != null && childId != null,
    staleTime: 10_000,
  });
}
