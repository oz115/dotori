import { useQuery } from '@tanstack/react-query';

import { storeApi } from '@/features/store/api/storeApi';
import { storeKeys } from '@/features/store/model/storeKeys';

/** 구매 내역 (SHOP-07). range 를 안 주면 전체 기간. */
export function usePurchaseHistory(
  familyId: number | undefined,
  childId: number | undefined,
  range?: { startDate: string; endDate: string },
) {
  return useQuery({
    queryKey: storeKeys.purchases(familyId ?? -1, childId ?? -1, range),
    queryFn: () => storeApi.getPurchaseHistory(familyId as number, childId as number, range),
    enabled: familyId != null && childId != null,
    staleTime: 10_000,
  });
}
