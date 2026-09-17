import { useQuery } from '@tanstack/react-query';

import { savingsApi } from '@/features/savings/api/savingsApi';
import { savingsKeys } from '@/features/savings/model/savingsKeys';

/** 가입 건의 만기 예상 수령액과 지금 해지 시 수령액. 상세 화면의 "만기에 받아요" 줄. */
export function useSavingsExpectedReturn(
  familyId: number | undefined,
  childId: number | undefined,
  savingsSubscriptionId: number | undefined,
) {
  return useQuery({
    queryKey: savingsKeys.expectedReturn(
      familyId ?? -1,
      childId ?? -1,
      savingsSubscriptionId ?? -1,
    ),
    queryFn: () =>
      savingsApi.getExpectedReturn(
        familyId as number,
        childId as number,
        savingsSubscriptionId as number,
      ),
    enabled: familyId != null && childId != null && savingsSubscriptionId != null,
  });
}
