import { useQuery } from '@tanstack/react-query';

import { storeApi } from '@/features/store/api/storeApi';
import { storeKeys } from '@/features/store/model/storeKeys';

/** 상점 상품 상세 (SHOP-02). */
export function useStoreProduct(familyId: number | undefined, productId: number | undefined) {
  return useQuery({
    queryKey: storeKeys.product(familyId ?? -1, productId ?? -1),
    queryFn: () => storeApi.getProduct(familyId as number, productId as number),
    enabled: familyId != null && productId != null,
    staleTime: 30_000,
  });
}
