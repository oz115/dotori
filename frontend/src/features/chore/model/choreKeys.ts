/** 집안일 관련 TanStack Query 키. */
export const choreKeys = {
  all: ['chore'] as const,
  /** 활성 집안일 목록 */
  list: (familyId: number, childId: number) =>
    [...choreKeys.all, 'list', familyId, childId] as const,
  /** 대기 중인 완료 요청 목록 */
  pendingRequests: (familyId: number, childId: number) =>
    [...choreKeys.all, 'pendingRequests', familyId, childId] as const,
  /** 기간별 완료 내역 */
  completions: (familyId: number, childId: number, startDate: string, endDate: string) =>
    [...choreKeys.all, 'completions', familyId, childId, startDate, endDate] as const,
};
