import { useQuery } from '@tanstack/react-query';

import { savingsApi } from '@/features/savings/api/savingsApi';
import { toSavingsTransactionItem } from '@/features/savings/lib/savingsView';
import { savingsKeys } from '@/features/savings/model/savingsKeys';

/** 예적금 거래 이력 (최신순). 가입·납입·수령·해지 성공 시 all 무효화로 함께 갱신된다. */
export function useSavingsTransactions(familyId: number | undefined, childId: number | undefined) {
  return useQuery({
    queryKey: savingsKeys.transactions(familyId ?? -1, childId ?? -1),
    queryFn: () => savingsApi.getTransactions(familyId as number, childId as number),
    enabled: familyId != null && childId != null,
    select: (transactions) => transactions.map(toSavingsTransactionItem),
  });
}
