/** 지갑 관련 TanStack Query 키. */
export const walletKeys = {
  all: ['wallet'] as const,
  /** 보유 포인트 (GET .../wallet/balance) */
  balance: (familyId: number, childId: number) =>
    [...walletKeys.all, 'balance', familyId, childId] as const,
};
