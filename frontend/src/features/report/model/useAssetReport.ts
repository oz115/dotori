import { useQuery } from '@tanstack/react-query';

import { reportApi } from '@/features/report/api/reportApi';
import { reportKeys } from '@/features/report/model/reportKeys';

/**
 * 자녀의 자산 현황 스냅샷 (RPT-04).
 *
 * 기간이 없어 주를 넘겨도 다시 부르지 않는다 — 키에 기간이 안 들어간다.
 */
export function useAssetReport(familyId: number | undefined, childId: number | undefined) {
  const enabled = familyId != null && childId != null;

  return useQuery({
    queryKey: reportKeys.assets(familyId ?? -1, childId ?? -1),
    queryFn: () => reportApi.getAssets(familyId as number, childId as number),
    enabled,
    staleTime: 30_000,
  });
}
