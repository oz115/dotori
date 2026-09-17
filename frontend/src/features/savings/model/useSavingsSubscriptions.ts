import { useQuery } from '@tanstack/react-query';

import { savingsApi } from '@/features/savings/api/savingsApi';
import { savingsKeys } from '@/features/savings/model/savingsKeys';

/** 아이의 예적금 가입 목록 — 카드의 진행 정보와 "보유한 것만 보기" 필터의 원천. */
export function useSavingsSubscriptions(familyId: number | undefined, childId: number | undefined) {
  return useQuery({
    queryKey: savingsKeys.subscriptions(familyId ?? -1, childId ?? -1),
    queryFn: () => savingsApi.getSubscriptions(familyId as number, childId as number),
    enabled: familyId != null && childId != null,
  });
}
