import { httpClient } from '@/shared/api';
import type {
  ChoreCompletionHistoryItem,
  ChoreCompletionSummary,
  ChoreSummary,
} from '@/features/chore/model/types';

const base = (familyId: number, childId: number) =>
  `/api/v1/families/${familyId}/children/${childId}/chores`;

/** 집안일 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const choreApi = {
  /** 활성 집안일 목록 */
  getChores: (familyId: number, childId: number) =>
    httpClient
      .get<{ chores: ChoreSummary[] }>(base(familyId, childId))
      .then((res) => res.data.chores),

  /** 아직 부모가 처리하지 않은(대기 중) 완료 요청 목록 */
  getPendingCompletionRequests: (familyId: number, childId: number) =>
    httpClient
      .get<{ requests: ChoreCompletionSummary[] }>(
        `${base(familyId, childId)}/completion-requests`,
        {
          params: { status: 'PENDING' },
        },
      )
      .then((res) => res.data.requests),

  /** 기간 내 승인·지급까지 끝난 완료 내역 */
  getCompletions: (familyId: number, childId: number, startDate: string, endDate: string) =>
    httpClient
      .get<{ completions: ChoreCompletionHistoryItem[] }>(
        `${base(familyId, childId)}/completions`,
        {
          params: { startDate, endDate },
        },
      )
      .then((res) => res.data.completions),
};
