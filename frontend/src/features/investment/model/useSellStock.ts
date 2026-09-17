import { useMutation, useQueryClient } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/** 매도 (INV-T-08) — 들고 있는 1주 전량. 성공 시 투자 캐시 전체 무효화. */
export function useSellStock() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: number) => investmentApi.sell(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: investmentKeys.all });
    },
  });
}
