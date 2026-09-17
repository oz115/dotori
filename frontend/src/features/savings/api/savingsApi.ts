import { httpClient } from '@/shared/api';
import type {
  SavingsExpectedReturn,
  SavingsProductResponse,
  SavingsSubscription,
  SavingsSummary,
  SavingsTransactionResponse,
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

  /** 상품 하나의 상세 (내용은 목록과 같다 — 상세 화면이 상품 하나만 열 때) */
  getProduct: (familyId: number, childId: number, savingsProductId: number) =>
    httpClient
      .get<SavingsProductResponse>(`${base(familyId, childId)}/products/${savingsProductId}`)
      .then((res) => res.data),

  /** 아이의 가입 목록 (진행 중·만기·완료 포함) */
  getSubscriptions: (familyId: number, childId: number) =>
    httpClient
      .get<SavingsSubscription[]>(`${base(familyId, childId)}/subscriptions`)
      .then((res) => res.data),

  /** 아이의 모든 가입 건 거래 이력 (최신순) */
  getTransactions: (familyId: number, childId: number) =>
    httpClient
      .get<SavingsTransactionResponse[]>(`${base(familyId, childId)}/transactions`)
      .then((res) => res.data),

  /** 만기 예상 수령액과 지금 해지 시 수령액 */
  getExpectedReturn: (familyId: number, childId: number, savingsSubscriptionId: number) =>
    httpClient
      .get<SavingsExpectedReturn>(
        `${base(familyId, childId)}/subscriptions/${savingsSubscriptionId}/expected-return`,
      )
      .then((res) => res.data),

  /** 가입 — amount 는 상품마다 의미가 다르다 (거치식=원금, 정액=회당, 목표=목표 금액) */
  subscribe: (
    familyId: number,
    childId: number,
    body: { savingsProductId: number; amount: number },
    idempotencyKey: string,
  ) =>
    httpClient
      .post<SavingsSubscription>(`${base(familyId, childId)}/subscriptions`, body, {
        headers: { 'Idempotency-Key': idempotencyKey },
      })
      .then((res) => res.data),

  /** 추가 납입 (정액 적립식·목표적금·보통예금) */
  contribute: (
    familyId: number,
    childId: number,
    savingsSubscriptionId: number,
    body: { amount: number },
    idempotencyKey: string,
  ) =>
    httpClient
      .post<SavingsSubscription>(
        `${base(familyId, childId)}/subscriptions/${savingsSubscriptionId}/contributions`,
        body,
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      .then((res) => res.data),

  /** 만기 수령 — 원금과 쌓인 이자가 지갑으로 들어온다 */
  receive: (
    familyId: number,
    childId: number,
    savingsSubscriptionId: number,
    idempotencyKey: string,
  ) =>
    httpClient
      .post<SavingsSubscription>(
        `${base(familyId, childId)}/subscriptions/${savingsSubscriptionId}/receipt`,
        undefined,
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      .then((res) => res.data),

  /** 중도 해지 — 원금만 돌려받고 쌓인 이자는 잃는다 */
  terminate: (
    familyId: number,
    childId: number,
    savingsSubscriptionId: number,
    idempotencyKey: string,
  ) =>
    httpClient
      .post<SavingsSubscription>(
        `${base(familyId, childId)}/subscriptions/${savingsSubscriptionId}/termination`,
        undefined,
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      .then((res) => res.data),
};
