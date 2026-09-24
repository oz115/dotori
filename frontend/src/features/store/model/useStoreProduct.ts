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
    // AI 가격 산정은 비동기라, 상세를 열어둔 채 완료될 수 있다. 가격이 아직 없으면(null)
    // 짧게 폴링해 산정이 끝나는 즉시 "산정 중" 대신 실제 가격이 뜨게 한다 (가격이 채워지면 멈춘다).
    refetchInterval: (query) => (query.state.data?.price == null ? 4000 : false),
  });
}
