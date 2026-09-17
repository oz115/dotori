import { useQuery } from '@tanstack/react-query';

import { investmentApi } from '@/features/investment/api/investmentApi';
import { toTransactionItem } from '@/features/investment/lib/mappers';
import { investmentKeys } from '@/features/investment/model/investmentKeys';

/** 매수/매도 거래 이력 (최신순). 매수/매도 성공 시 investmentKeys.all 무효화로 함께 갱신된다. */
export function useInvestmentTransactions() {
  return useQuery({
    queryKey: investmentKeys.transactions(),
    queryFn: investmentApi.getTransactions,
    select: (transactions) => transactions.map(toTransactionItem),
  });
}
