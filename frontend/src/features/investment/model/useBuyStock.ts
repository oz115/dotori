import { useMutation, useQueryClient } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/**
 * 매수 (INV-T-07). 종목당 1주 규칙이라 수량 없이 종목만 보낸다.
 * 성공하면 보유·요약·상세가 모두 바뀌므로 투자 캐시 전체를 무효화한다.
 * 지갑 잔액은 이 feature 가 모르는 슬라이스라 호출하는 라우트가 따로 처리한다.
 */
export function useBuyStock() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: number) => investmentApi.buy(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: investmentKeys.all });
    },
  });
}
