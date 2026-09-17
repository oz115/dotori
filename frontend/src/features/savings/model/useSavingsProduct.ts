import { useQuery } from '@tanstack/react-query';

import { savingsApi } from '@/features/savings/api/savingsApi';
import { savingsKeys } from '@/features/savings/model/savingsKeys';

/** 상품 하나의 상세. 스펙은 고정이라 목록과 같은 staleTime 을 쓴다. */
export function useSavingsProduct(
  familyId: number | undefined,
  childId: number | undefined,
  savingsProductId: number | undefined,
) {
  return useQuery({
    queryKey: savingsKeys.product(familyId ?? -1, childId ?? -1, savingsProductId ?? -1),
    queryFn: () =>
      savingsApi.getProduct(familyId as number, childId as number, savingsProductId as number),
    enabled: familyId != null && childId != null && savingsProductId != null,
    staleTime: 60_000,
  });
}
