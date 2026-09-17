import { httpClient } from '@/shared/api';
import type { SavingsSummary } from '@/features/savings/model/types';

const base = (familyId: number, childId: number) =>
  `/api/v1/families/${familyId}/children/${childId}/savings`;

/** 예적금 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const savingsApi = {
  /** 가입 현황 요약. 조회라 아이 본인과 그 가족의 부모 모두 부를 수 있다 */
  getSummary: (familyId: number, childId: number) =>
    httpClient.get<SavingsSummary>(`${base(familyId, childId)}/summary`).then((res) => res.data),
};
