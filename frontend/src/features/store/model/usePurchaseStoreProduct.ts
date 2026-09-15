import { useMutation, useQueryClient } from '@tanstack/react-query';

import { storeApi } from '@/features/store/api/storeApi';
import { storeKeys } from '@/features/store/model/storeKeys';

/**
 * 상품 구매 (SHOP-06). 수량은 항상 1 — 하루 1회 제한 상품을 여러 개 사는 흐름 자체가 없다.
 * 성공하면 재고·가격이 바뀌었을 수 있어 목록·상세 캐시를 무효화한다. 지갑 잔액은 이
 * feature 가 모르는 슬라이스라 여기서 안 건드린다 — 호출하는 라우트가 따로 처리한다.
 */
export function usePurchaseStoreProduct(
  familyId: number | undefined,
  productId: number | undefined,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => storeApi.purchaseProduct(familyId as number, productId as number, 1),
    onSuccess: () => {
      if (familyId == null) return;
      queryClient.invalidateQueries({ queryKey: storeKeys.products(familyId) });
      if (productId != null) {
        queryClient.invalidateQueries({ queryKey: storeKeys.product(familyId, productId) });
      }
    },
  });
}
