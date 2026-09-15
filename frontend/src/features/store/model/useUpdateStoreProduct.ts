import { useMutation, useQueryClient } from '@tanstack/react-query';

import { storeApi } from '@/features/store/api/storeApi';
import { storeKeys } from '@/features/store/model/storeKeys';
import type { StoreProductUpdateBody } from '@/features/store/model/types';

/** 상품 수정 (SHOP-04). 성공하면 목록·상세 캐시를 둘 다 무효화한다. */
export function useUpdateStoreProduct(familyId: number | undefined, productId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: StoreProductUpdateBody) =>
      storeApi.updateProduct(familyId as number, productId as number, body),
    onSuccess: () => {
      if (familyId == null) return;
      queryClient.invalidateQueries({ queryKey: storeKeys.products(familyId) });
      if (productId != null) {
        queryClient.invalidateQueries({ queryKey: storeKeys.product(familyId, productId) });
      }
    },
  });
}
