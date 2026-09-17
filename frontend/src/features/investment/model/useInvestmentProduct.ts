import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/**
 * 종목 상세 (서버 응답 그대로). 화면용 StockDetail 로의 조합은 가격 이력·보유 여부까지
 * 필요해서 라우트가 useStockDetail 훅으로 묶어 쓴다.
 */
export function useInvestmentProduct(productId: number | undefined) {
  return useQuery({
    queryKey: investmentKeys.product(productId ?? -1),
    queryFn: () => investmentApi.getProduct(productId as number),
    enabled: productId != null,
    staleTime: 60_000,
  });
}
