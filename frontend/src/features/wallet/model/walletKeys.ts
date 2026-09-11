/** 지갑 관련 TanStack Query 키. */
export const walletKeys = {
  all: ['wallet'] as const,
  /** 보유 포인트 (GET .../wallet/balance) */
  balance: (familyId: number, childId: number) =>
    [...walletKeys.all, 'balance', familyId, childId] as const,
  /** 환전 요청 목록 (GET .../wallet/cashout-requests) */
  cashoutRequests: (familyId: number, childId: number, status: string) =>
    [...walletKeys.all, 'cashoutRequests', familyId, childId, status] as const,
};
