import { useMutation, useQueryClient } from '@tanstack/react-query';

import { storeApi } from '@/features/store/api/storeApi';
import { storeKeys } from '@/features/store/model/storeKeys';
import type { StoreProductCreateBody } from '@/features/store/model/types';

/** 상품 등록 (SHOP-03). 성공하면 목록 캐시를 무효화해 새 상품이 바로 보이게 한다. */
export function useCreateStoreProduct(familyId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: StoreProductCreateBody) => storeApi.createProduct(familyId as number, body),
    onSuccess: () => {
      if (familyId != null) {
        queryClient.invalidateQueries({ queryKey: storeKeys.products(familyId) });
      }
    },
  });
}
