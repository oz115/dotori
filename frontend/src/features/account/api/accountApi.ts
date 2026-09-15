import { httpClient } from '@/shared/api';
import type {
  AccountAuthBody,
  AccountAuthResponse,
  AccountInfo,
  AccountVerifyBody,
  LinkedAccount,
  TransferBody,
  TransferResult,
} from '@/features/account/model/types';

/**
 * 계좌(1원 인증) 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당.
 */
export const accountApi = {
  /** 1원 송금 요청 — 이미 연동돼 있으면 409 ACCOUNT_ALREADY_LINKED */
  requestAuth: (body: AccountAuthBody) =>
    httpClient.post<AccountAuthResponse>('/api/v1/accounts/auth', body).then((res) => res.data),

  /** 1원 인증 코드 검증 — 불일치면 AUTH_CODE_MISMATCH, 성공 시 201 연동 완료 */
  verifyAuth: (body: AccountVerifyBody) =>
    httpClient.post<LinkedAccount>('/api/v1/accounts/auth/verify', body).then((res) => res.data),

  /** 연동 계좌 상세 (금융망 실시간). 연동 없으면 ACCOUNT_NOT_LINKED */
  getMyAccount: () => httpClient.get<AccountInfo>('/api/v1/accounts/me').then((res) => res.data),

  /** 계좌 연동 해제 — 성공 시 204 */
  unlink: () => httpClient.delete('/api/v1/accounts/me'),

  /** 연동 계좌 → 임의 계좌 이체. 자기 계좌면 SELF_TRANSFER_NOT_ALLOWED */
  transfer: (body: TransferBody, idempotencyKey: string) =>
    httpClient
      .post<TransferResult>('/api/v1/accounts/transfer', body, {
        headers: { 'Idempotency-Key': idempotencyKey },
      })
      .then((res) => res.data),
};
