/** 예적금 관련 TanStack Query 키. */
export const savingsKeys = {
  all: ['savings'] as const,
  /** 부모 홈 카드용 요약 (GET .../savings/summary) */
  summary: (familyId: number, childId: number) =>
    [...savingsKeys.all, 'summary', familyId, childId] as const,
};
