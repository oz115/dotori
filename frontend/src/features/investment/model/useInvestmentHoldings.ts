import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { toHolding } from '@/features/investment/lib/mappers';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/**
 * 보유 종목 목록. childId 없으면 본인(자녀용)이고, 부모가 자녀 것을 볼 때만 넘긴다.
 * 요약 카드(넣은 돈/번 돈)와 상세 화면의 "보유중" 배지도 이 데이터에서 나온다.
 */
export function useInvestmentHoldings(childId?: number) {
  return useQuery({
    queryKey: investmentKeys.holdings(childId),
    queryFn: () => investmentApi.getHoldings(childId),
    select: (holdings) => holdings.map(toHolding),
  });
}
