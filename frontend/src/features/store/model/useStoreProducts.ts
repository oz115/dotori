import { useQuery } from '@tanstack/react-query';

import { storeApi } from '@/features/store/api/storeApi';
import { storeKeys } from '@/features/store/model/storeKeys';

/**
 * 우리 가족 상점에 올라온 상품 목록.
 * familyId 는 다른 슬라이스(family) 값이라 호출하는 쪽(widget)이 넘겨준다.
 */
export function useStoreProducts(familyId: number | undefined) {
  return useQuery({
    queryKey: storeKeys.products(familyId ?? -1),
    queryFn: () => storeApi.getProducts(familyId as number),
    enabled: familyId != null,
    staleTime: 30_000,
  });
}
