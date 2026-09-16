import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { toStockSummary } from '@/features/investment/lib/mappers';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/** 투자 종목 목록. 가격이 하루 한 번(00:05 KST)만 움직여 staleTime 을 넉넉히 둔다. */
export function useInvestmentProducts() {
  return useQuery({
    queryKey: investmentKeys.products(),
    queryFn: investmentApi.getProducts,
    select: (products) => products.map(toStockSummary),
    staleTime: 60_000,
  });
}
