/** 퀴즈 관련 TanStack Query 키. */
export const quizKeys = {
  all: ['quiz'] as const,
  today: (familyId: number, childId: number) =>
    [...quizKeys.all, 'today', familyId, childId] as const,
};
