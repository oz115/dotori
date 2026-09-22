import { httpClient } from '@/shared/api';
import type {
  CashoutBody,
  CashoutRequest,
  CashoutStatus,
  ChargeBody,
  PointBalance,
  PointTransaction,
  PointTransactionHistory,
} from '@/features/wallet/model/types';

const base = (familyId: number, childId: number) =>
  `/api/v1/families/${familyId}/children/${childId}/wallet`;

/** 포인트 지갑 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const walletApi = {
  /** 보유 포인트 조회. 지갑이 아직 없어도 오류가 아니라 잔액 0으로 온다 */
  getBalance: (familyId: number, childId: number) =>
    httpClient.get<PointBalance>(`${base(familyId, childId)}/balance`).then((res) => res.data),

  /** 실제 계좌에서 송금 + 포인트 발행. amount 는 원, 10원 단위 */
  charge: (familyId: number, childId: number, body: ChargeBody, idempotencyKey: string) =>
    httpClient
      .post<PointTransaction>(`${base(familyId, childId)}/charge`, body, {
        headers: { 'Idempotency-Key': idempotencyKey },
      })
      .then((res) => res.data),

  /**
   * 포인트 거래내역 (WLT-06). 기간은 서버가 거르고, 무한 스크롤은 커서로 잇는다 —
   * 첫 요청은 cursor 없이, 다음부터는 응답의 page.nextCursor 를 그대로 싣는다.
   */
  getTransactions: (
    familyId: number,
    childId: number,
    params: { startDate?: string; endDate?: string; cursor?: string; size?: number },
  ) =>
    httpClient
      .get<PointTransactionHistory>(`${base(familyId, childId)}/transactions`, { params })
      .then((res) => res.data),

  /** 환전 요청. amount 는 포인트 — 요청 즉시 차감되고, 부모 승인 후 실제 송금된다 */
  requestCashout: (familyId: number, childId: number, body: CashoutBody, idempotencyKey: string) =>
    httpClient
      .post<CashoutRequest>(`${base(familyId, childId)}/cashout-requests`, body, {
        headers: { 'Idempotency-Key': idempotencyKey },
      })
      .then((res) => res.data),

  /** 환전 요청 목록. 자녀 본인과 그 가족의 부모가 볼 수 있다. status 생략 시 전체 */
  getCashoutRequests: (familyId: number, childId: number, status?: CashoutStatus) =>
    httpClient
      .get<CashoutRequest[]>(`${base(familyId, childId)}/cashout-requests`, {
        params: status ? { status } : undefined,
      })
      .then((res) => res.data),

  /** 환전 승인 (부모). 부모 계좌에서 자녀 계좌로 실제 송금이 일어난다 */
  approveCashout: (
    familyId: number,
    childId: number,
    cashoutRequestId: number,
    idempotencyKey: string,
  ) =>
    httpClient
      .post<CashoutRequest>(
        `${base(familyId, childId)}/cashout-requests/${cashoutRequestId}/approve`,
        null,
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      .then((res) => res.data),

  /** 환전 거절 (부모). 요청 때 차감했던 포인트를 서버가 자녀에게 환급한다 */
  declineCashout: (
    familyId: number,
    childId: number,
    cashoutRequestId: number,
    idempotencyKey: string,
  ) =>
    httpClient
      .post<CashoutRequest>(
        `${base(familyId, childId)}/cashout-requests/${cashoutRequestId}/decline`,
        null,
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      .then((res) => res.data),
};
