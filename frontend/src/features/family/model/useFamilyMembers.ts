import { useQuery } from '@tanstack/react-query';

import { familyApi } from '@/features/family/api/familyApi';
import { familyKeys } from '@/features/family/model/familyKeys';

/**
 * 가족 구성원 목록. 부모 홈이 "내 아이가 누구인지"(childId·이름)를 알아내는 유일한 경로다 —
 * 지갑·집안일 조회 경로가 전부 childId 를 요구하기 때문.
 */
export function useFamilyMembers(
  familyId: number | undefined,
  /** 아이가 합류하기를 기다리는 화면에서만 준다 — 이 주기(ms)로 다시 조회한다 */
  refetchIntervalMs?: number,
) {
  return useQuery({
    queryKey: familyKeys.members(familyId ?? -1),
    queryFn: () => familyApi.getMembers(familyId as number),
    enabled: familyId != null,
    staleTime: 60_000,
    refetchInterval: refetchIntervalMs,
  });
}
