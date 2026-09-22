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
    // 상세와 같은 이유 — 가격 산정 중(price null)인 상품이 하나라도 있으면 짧게 폴링해
    // 산정이 끝나는 즉시 목록에도 실제 가격이 반영되게 한다 (모두 산정되면 멈춘다).
    refetchInterval: (query) =>
      query.state.data?.some((product) => product.price == null) ? 4000 : false,
  });
}
