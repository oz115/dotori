import { useQuery } from '@tanstack/react-query';

import { savingsApi } from '@/features/savings/api/savingsApi';
import { savingsKeys } from '@/features/savings/model/savingsKeys';

/** 예적금 상품 6종. 상품 스펙은 고정이라 넉넉히 캐시한다. */
export function useSavingsProducts(familyId: number | undefined, childId: number | undefined) {
  return useQuery({
    queryKey: savingsKeys.products(familyId ?? -1, childId ?? -1),
    queryFn: () => savingsApi.getProducts(familyId as number, childId as number),
    enabled: familyId != null && childId != null,
    staleTime: 60_000,
  });
}
