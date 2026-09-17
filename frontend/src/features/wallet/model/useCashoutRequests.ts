import { useQuery } from '@tanstack/react-query';

import { walletApi } from '@/features/wallet/api/walletApi';
import { walletKeys } from '@/features/wallet/model/walletKeys';
import type { CashoutStatus } from '@/features/wallet/model/types';

/**
 * 환전 요청 목록. 부모 홈은 승인 대기(PENDING) 건수를 세는 데 쓴다.
 *
 * status 를 넘기지 않으면 전체를 받는다 — 대기 중과 지난 내역을 함께 보여주는 화면이
 * 상태별로 두 번 부르지 않게 하려는 것이다.
 */
export function useCashoutRequests(
  familyId: number | undefined,
  childId: number | undefined,
  status?: CashoutStatus,
) {
  return useQuery({
    queryKey: walletKeys.cashoutRequests(familyId ?? -1, childId ?? -1, status),
    queryFn: () => walletApi.getCashoutRequests(familyId as number, childId as number, status),
    enabled: familyId != null && childId != null,
    staleTime: 10_000,
  });
}
