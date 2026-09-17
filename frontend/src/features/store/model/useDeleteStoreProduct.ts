import { useMutation, useQueryClient } from '@tanstack/react-query';

import { storeApi } from '@/features/store/api/storeApi';
import { storeKeys } from '@/features/store/model/storeKeys';

/** 상품 삭제 (SHOP-05, soft delete). 성공하면 목록 캐시를 무효화한다. */
export function useDeleteStoreProduct(familyId: number | undefined, productId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => storeApi.deleteProduct(familyId as number, productId as number),
    onSuccess: () => {
      if (familyId != null) {
        queryClient.invalidateQueries({ queryKey: storeKeys.products(familyId) });
      }
    },
  });
}
