import { httpClient } from '@/shared/api';
import type {
  AssetReport,
  IncomeReport,
  IncomeType,
  ReportPeriod,
  SpendingReport,
} from '@/features/report/model/types';

const base = (familyId: number, childId: number) =>
  `/api/v1/families/${familyId}/children/${childId}/reports`;

/**
 * 리포트 원시 HTTP 호출 (RPT-02·03·04). 세션·토큰은 httpClient 인터셉터가,
 * 캐시 갱신은 model 훅이 담당한다.
 *
 * 조회 권한은 그 가족의 부모 또는 자녀 본인이다 — 셋 다 같은 규칙이다.
 */
export const reportApi = {
  /**
   * 수입 통계. `incomeType` 을 주면 total·byType·byPeriod 가 모두 그 사유 기준이 된다.
   * 수입이 아닌 사유(PURCHASE 등)를 주면 서버가 400 으로 막는다.
   */
  getIncome: (familyId: number, childId: number, period: ReportPeriod, incomeType?: IncomeType) =>
    httpClient
      .get<IncomeReport>(`${base(familyId, childId)}/income`, {
        params: { ...period, incomeType },
      })
      .then((res) => res.data),

  /** 소비 통계. 명세에 유형 필터가 없어 기간만 받는다 */
  getSpending: (familyId: number, childId: number, period: ReportPeriod) =>
    httpClient
      .get<SpendingReport>(`${base(familyId, childId)}/spending`, { params: period })
      .then((res) => res.data),

  /** 자산 현황. 기간 파라미터가 없는 현재 시점 스냅샷이다 */
  getAssets: (familyId: number, childId: number) =>
    httpClient.get<AssetReport>(`${base(familyId, childId)}/assets`).then((res) => res.data),
};
