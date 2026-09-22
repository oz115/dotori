/**
 * 도토리 포인트 지갑 관련 서버 계약.
 * 경로가 `/families/{familyId}/children/{childId}/wallet/...` 라서 familyId·childId 를
 * 함께 넘겨야 한다 — familyId 는 `useFamilyId`, childId 는 본인 userId 를 쓴다.
 */

/** GET .../wallet/balance 응답 */
export type PointBalance = {
  childId: number;
  balance: number;
  /** 잔액이 마지막으로 바뀐 시각. 지갑이 아직 없으면 null */
  updatedAt: string | null;
};

/** POST .../wallet/charge 요청 — 실제 계좌에서 보낼 금액(원). 10원 단위여야 한다 */
export type ChargeBody = {
  amount: number;
};

/** POST .../wallet/charge 응답 (201) */
export type PointTransaction = {
  pointTransactionId: number;
  transactionType: string;
  amount: number;
  balanceAfter: number;
  createdAt: string;
};

/**
 * POST .../wallet/grant 요청 — 부모가 자녀에게 주는 도토리(포인트). 실제 송금 없이 발행된다.
 * 응답은 충전과 같은 PointTransaction (transactionType 이 GRANT).
 */
export type GrantBody = {
  /** 줄 포인트 (1 이상) */
  amount: number;
  /** 거래내역에 그대로 보이는 사유 (255자 이하). 생략하면 서버가 "부모 지급"으로 채운다 */
  description?: string;
};

/** 포인트가 움직인 사유 (point_transactions.transaction_type 라벨) */
export type PointTransactionReason =
  | 'CHARGE'
  | 'GRANT'
  | 'ALLOWANCE'
  | 'PURCHASE'
  | 'CASHOUT'
  | 'SAVINGS_DEPOSIT'
  | 'SAVINGS_WITHDRAWAL'
  | 'INVESTMENT_BUY'
  | 'INVESTMENT_SELL'
  | 'QUIZ_REWARD'
  | 'CHORE_REWARD'
  | 'REFUND';

/**
 * GET .../wallet/transactions 목록 한 줄 (WLT-06).
 * amount 는 항상 양수고 방향은 transactionDirection 이 알려준다 (서버가 사유에서 계산).
 */
export type PointTransactionItem = {
  pointTransactionId: number;
  transactionType: PointTransactionReason;
  transactionDirection: 'IN' | 'OUT' | null;
  amount: number;
  balanceAfter: number;
  description: string | null;
  createdAt: string;
};

/** GET .../wallet/transactions 응답 — 무한 스크롤은 page.nextCursor 를 이어 보낸다 */
export type PointTransactionHistory = {
  childId: number;
  balance: number;
  page: {
    items: PointTransactionItem[];
    nextCursor: string | null;
    hasNext: boolean;
  };
};

/** POST .../wallet/cashout-requests 요청 — 현금으로 바꿀 포인트 수량 */
export type CashoutBody = {
  amount: number;
};

export type CashoutStatus =
  'PENDING' | 'PROGRESS' | 'CANCELLED' | 'DECLINED' | 'SUCCEED' | 'FAILED';

/** POST .../wallet/cashout-requests 응답 (201) */
export type CashoutRequest = {
  cashoutRequestId: number;
  childId: number;
  parentId: number;
  pointAmount: number;
  status: CashoutStatus;
  requestedAt: string;
  processedAt: string | null;
};
