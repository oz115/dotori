import { useQuery } from '@tanstack/react-query';

import { reportApi } from '@/features/report/api/reportApi';
import { reportKeys } from '@/features/report/model/reportKeys';
import type { IncomeType, ReportPeriod } from '@/features/report/model/types';

/**
 * 자녀의 기간별 수입 통계 (RPT-02).
 *
 * familyId·childId 는 다른 슬라이스(family·auth) 값이라 이 feature 가 직접 읽지 않고
 * 호출하는 쪽(widget)이 넘겨준다 — features 는 서로 직접 import 하지 않는다.
 */
export function useIncomeReport(
  familyId: number | undefined,
  childId: number | undefined,
  period: ReportPeriod,
  incomeType?: IncomeType,
) {
  const enabled = familyId != null && childId != null;

  return useQuery({
    queryKey: reportKeys.income(familyId ?? -1, childId ?? -1, period, incomeType),
    queryFn: () => reportApi.getIncome(familyId as number, childId as number, period, incomeType),
    enabled,
    staleTime: 30_000,
  });
}
