import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { toTransactionItems } from '@/features/investment/lib/transactions';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/**
 * 매수/매도 거래 이력 (최신순). 매수/매도 성공 시 investmentKeys.all 무효화로 함께 갱신된다.
 * childId 없으면 본인(자녀용)이고, 부모가 자녀 것을 볼 때만 넘긴다. 부모 화면은 가족 조회가
 * 끝나야 childId 를 알 수 있어, 그 전에 호출하면 "부모 본인 거래" 조회가 되므로
 * 호출하는 쪽이 enabled 로 childId 가 준비될 때까지 막는다 (useInvestmentHoldings 와 같다).
 */
export function useInvestmentTransactions(childId?: number, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: investmentKeys.transactions(childId),
    queryFn: () => investmentApi.getTransactions(childId),
    select: toTransactionItems,
    retry: false,
    enabled: options?.enabled ?? true,
  });
}
