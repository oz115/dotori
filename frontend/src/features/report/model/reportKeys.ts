import type { ReportPeriod } from '@/features/report/model/types';

/**
 * 리포트 관련 TanStack Query 키.
 *
 * 수입·소비는 기간이 키에 들어간다 — 주를 넘길 때마다 다른 캐시 항목이라, 앞뒤로
 * 오가도 이미 본 주는 다시 부르지 않는다. 자산은 기간이 없어 자녀당 하나다.
 */
export const reportKeys = {
  all: ['report'] as const,
  income: (familyId: number, childId: number, period: ReportPeriod, incomeType?: string) =>
    [
      ...reportKeys.all,
      'income',
      familyId,
      childId,
      period.startDate,
      period.endDate,
      incomeType ?? 'ALL',
    ] as const,
  spending: (familyId: number, childId: number, period: ReportPeriod) =>
    [...reportKeys.all, 'spending', familyId, childId, period.startDate, period.endDate] as const,
  assets: (familyId: number, childId: number) =>
    [...reportKeys.all, 'assets', familyId, childId] as const,
};
