import { useQuery } from '@tanstack/react-query';

import { reportApi } from '@/features/report/api/reportApi';
import { reportKeys } from '@/features/report/model/reportKeys';
import type { ReportPeriod } from '@/features/report/model/types';

/** 자녀의 기간별 소비 통계 (RPT-03). */
export function useSpendingReport(
  familyId: number | undefined,
  childId: number | undefined,
  period: ReportPeriod,
) {
  const enabled = familyId != null && childId != null;

  return useQuery({
    queryKey: reportKeys.spending(familyId ?? -1, childId ?? -1, period),
    queryFn: () => reportApi.getSpending(familyId as number, childId as number, period),
    enabled,
    staleTime: 30_000,
  });
}
