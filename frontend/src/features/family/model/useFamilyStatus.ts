import { useQuery } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';
import { familyKeys } from '@/features/family/model/familyKeys';

/**
 * 현재 사용자의 가족 소속 여부. `GET /api/v1/families/status`.
 * 온보딩 진입 분기(가족 없음 → 온보딩 / 있음 → 홈)와 부모 홈의 임시 삭제 버튼 노출에 쓴다.
 */
export function useFamilyStatus() {
  return useQuery({
    queryKey: familyKeys.status(),
    queryFn: () => familyApi.getStatus(),
    staleTime: 60_000,
  });
}
