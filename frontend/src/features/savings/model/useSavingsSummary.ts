import { useQuery } from '@tanstack/react-query';

import { savingsApi } from '@/features/savings/api/savingsApi';
import { savingsKeys } from '@/features/savings/model/savingsKeys';

/**
 * 자녀의 예적금 가입 현황 요약. 부모 홈 카드가 쓰고, 아이 본인도 같은 API 로 조회한다.
 *
 * familyId·childId 는 다른 슬라이스(family·auth) 값이라 이 feature 가 직접 읽지 않고
 * 호출하는 쪽(widget)이 넘겨준다 — features 는 서로 직접 import 하지 않는다.
 */
export function useSavingsSummary(familyId: number | undefined, childId: number | undefined) {
  const enabled = familyId != null && childId != null;

  return useQuery({
    queryKey: savingsKeys.summary(familyId ?? -1, childId ?? -1),
    queryFn: () => savingsApi.getSummary(familyId as number, childId as number),
    enabled,
    staleTime: 30_000,
  });
}
