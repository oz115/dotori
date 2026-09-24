import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { toHolding } from '@/features/investment/lib/mappers';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/**
 * 보유 종목 목록. childId 없으면 본인(자녀용)이고, 부모가 자녀 것을 볼 때만 넘긴다.
 * 요약 카드(넣은 돈/번 돈)와 상세 화면의 "보유중" 배지도 이 데이터에서 나온다.
 *
 * 부모 화면은 가족 조회가 끝나야 childId 를 알 수 있다. 그 전에 그냥 호출하면
 * childId 없는 요청 = "부모 본인 보유" 조회가 되어 엉뚱한 빈 목록이 잠깐 보이므로,
 * 호출하는 쪽이 enabled 로 childId 가 준비될 때까지 막는다.
 */
export function useInvestmentHoldings(childId?: number, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: investmentKeys.holdings(childId),
    queryFn: () => investmentApi.getHoldings(childId),
    select: (holdings) => holdings.map(toHolding),
    enabled: options?.enabled ?? true,
  });
}
