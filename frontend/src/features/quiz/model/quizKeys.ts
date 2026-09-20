/** 퀴즈 관련 TanStack Query 키. */
export const quizKeys = {
  all: ['quiz'] as const,
  today: (familyId: number, childId: number) =>
    [...quizKeys.all, 'today', familyId, childId] as const,
  /** 부모용 오늘의 퀴즈 확인 — 아이용 today 캐시와 섞이지 않게 키를 나눈다 */
  parentToday: (familyId: number, childId: number) =>
    [...quizKeys.all, 'parent-today', familyId, childId] as const,
};
