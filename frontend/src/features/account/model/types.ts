/**
 * 1원 인증(계좌 연동) 관련 서버 계약.
 * 은행 정보는 프론트가 `lib/banks` 에서 관리하고, 서버엔 code·name·accountNo 만 보낸다.
 */

/** POST /api/v1/accounts/auth 요청 — 1원 송금 */
export type AccountAuthBody = {
  bankCode: string;
  bankName: string;
  /** '-' 없이 숫자만 */
  accountNo: string;
};

/** POST /api/v1/accounts/auth 응답 — authText 뒤 4자리가 인증 코드 */
export type AccountAuthResponse = {
  accountNo: string;
  /** 입금자명 앞부분 (예: "도토리") */
  authText: string;
  /** 인증 세션 만료까지 남은 초 */
  expiresInSeconds: number;
};

/** POST /api/v1/accounts/auth/verify 요청 */
export type AccountVerifyBody = AccountAuthBody & {
  /** 입금내역에서 확인한 authText 뒤 숫자 */
  authCode: string;
};

/** POST /api/v1/accounts/auth/verify 응답 (201) — 연동 완료 */
export type LinkedAccount = {
  id: number;
  bankCode: string;
  bankName: string;
  accountNo: string;
  createdAt: string;
};

/** GET /api/v1/accounts/me 응답 — 연동 계좌 상세 (금융망 실시간) */
export type AccountInfo = {
  bankCode: string;
  bankName: string;
  accountNo: string;
  accountName: string;
  accountTypeName: string;
  accountBalance: number;
  dailyTransferLimit: number;
  oneTimeTransferLimit: number;
  currency: string;
};

/**
 * POST /api/v1/accounts/transfer 요청.
 * toAccountNo 는 연동 계좌로 한정되지 않는 임의의 은행 계좌번호 — 은행 코드는 받지 않는다
 * (SSAFY 금융망은 계좌번호가 전역 유일해 은행 구분이 필요 없다).
 */
export type TransferBody = {
  toAccountNo: string;
  /** 원 단위 */
  amount: number;
  /** 생략하면 서버가 "계좌 이체" 로 채운다 */
  memo?: string;
};

/** POST /api/v1/accounts/transfer 응답 */
export type TransferResult = {
  fromAccountNo: string;
  toAccountNo: string;
  amount: number;
  transactedAt: string;
};
