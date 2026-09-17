/** 예적금 관련 TanStack Query 키. 가입·납입 후에는 all 로 한꺼번에 무효화한다. */
export const savingsKeys = {
  all: ['savings'] as const,
  /** 부모 홈 카드용 요약 (GET .../savings/summary) */
  summary: (familyId: number, childId: number) =>
    [...savingsKeys.all, 'summary', familyId, childId] as const,
  products: (familyId: number, childId: number) =>
    [...savingsKeys.all, 'products', familyId, childId] as const,
  product: (familyId: number, childId: number, savingsProductId: number) =>
    [...savingsKeys.all, 'products', familyId, childId, savingsProductId] as const,
  subscriptions: (familyId: number, childId: number) =>
    [...savingsKeys.all, 'subscriptions', familyId, childId] as const,
  transactions: (familyId: number, childId: number) =>
    [...savingsKeys.all, 'transactions', familyId, childId] as const,
  expectedReturn: (familyId: number, childId: number, savingsSubscriptionId: number) =>
    [...savingsKeys.all, 'expected-return', familyId, childId, savingsSubscriptionId] as const,
};
