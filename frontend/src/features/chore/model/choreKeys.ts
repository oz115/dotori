import type { ChoreCompletionStatus } from '@/features/chore/model/types';

/** 집안일 관련 TanStack Query 키. */
export const choreKeys = {
  all: ['chore'] as const,
  /** 활성 집안일 목록 */
  list: (familyId: number, childId: number) =>
    [...choreKeys.all, 'list', familyId, childId] as const,
  /** 집안일 상세 */
  detail: (familyId: number, childId: number, choreId: number) =>
    [...choreKeys.all, 'detail', familyId, childId, choreId] as const,
  /**
   * 완료 요청 목록. 서버가 상태로 걸러 주므로 상태까지 키에 담는다.
   *
   * 상태를 빼면 그 아래 전부를 가리키는 접두사가 된다. 무효화는 이 접두사로 한다 -
   * 승인·거절은 대기 목록과 거절 목록을 동시에 바꾸므로, 한쪽만 털면 다른 쪽이 낡는다.
   */
  completionRequests: (familyId: number, childId: number, status?: ChoreCompletionStatus) =>
    status == null
      ? ([...choreKeys.all, 'completionRequests', familyId, childId] as const)
      : ([...choreKeys.all, 'completionRequests', familyId, childId, status] as const),
  /** 기간별 완료 내역 */
  completions: (familyId: number, childId: number, startDate: string, endDate: string) =>
    [...choreKeys.all, 'completions', familyId, childId, startDate, endDate] as const,
};
