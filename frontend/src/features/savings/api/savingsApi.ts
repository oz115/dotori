import { httpClient } from '@/shared/api';
import type {
  SavingsProductResponse,
  SavingsSubscription,
  SavingsSummary,
} from '@/features/savings/model/types';

const base = (familyId: number, childId: number) =>
  `/api/v1/families/${familyId}/children/${childId}/savings`;

/** 예적금 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const savingsApi = {
  /** 가입 현황 요약. 조회라 아이 본인과 그 가족의 부모 모두 부를 수 있다 */
  getSummary: (familyId: number, childId: number) =>
    httpClient.get<SavingsSummary>(`${base(familyId, childId)}/summary`).then((res) => res.data),

  /** 6개 고정 상품 + 이 가족 기준 가입 가능 최대 금액 */
  getProducts: (familyId: number, childId: number) =>
    httpClient
      .get<SavingsProductResponse[]>(`${base(familyId, childId)}/products`)
      .then((res) => res.data),

  /** 아이의 가입 목록 (진행 중·만기·완료 포함) */
  getSubscriptions: (familyId: number, childId: number) =>
    httpClient
      .get<SavingsSubscription[]>(`${base(familyId, childId)}/subscriptions`)
      .then((res) => res.data),
};
