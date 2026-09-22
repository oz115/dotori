/** 지갑 관련 TanStack Query 키. */
export const walletKeys = {
  all: ['wallet'] as const,
  /** 보유 포인트 (GET .../wallet/balance) */
  balance: (familyId: number, childId: number) =>
    [...walletKeys.all, 'balance', familyId, childId] as const,
  /** 포인트 거래내역 (GET .../wallet/transactions). 기간이 다르면 다른 목록이다 */
  transactions: (
    familyId: number,
    childId: number,
    range: { startDate: string; endDate: string } | null,
  ) =>
    [
      ...walletKeys.all,
      'transactions',
      familyId,
      childId,
      range?.startDate ?? 'ALL',
      range?.endDate ?? 'ALL',
    ] as const,
  /**
   * 환전 요청 목록 (GET .../wallet/cashout-requests).
   * 상태를 빼면 자녀의 모든 상태 목록 하나를 가리킨다 — 승인·거절 후 상태별 캐시를
   * 한꺼번에 털 때는 `cashoutRequestsOf` 접두사를 쓴다.
   */
  cashoutRequests: (familyId: number, childId: number, status: string = 'ALL') =>
    [...walletKeys.cashoutRequestsOf(familyId, childId), status] as const,
  /** 한 자녀의 환전 요청 목록 전체(상태 불문) 무효화용 접두사 */
  cashoutRequestsOf: (familyId: number, childId: number) =>
    [...walletKeys.all, 'cashoutRequests', familyId, childId] as const,
};
