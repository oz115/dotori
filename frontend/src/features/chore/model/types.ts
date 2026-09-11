/**
 * 집안일(할 일) 관련 서버 계약. 경로가 `/families/{familyId}/children/{childId}/chores...`
 * 라서 wallet 과 마찬가지로 familyId·childId 를 함께 넘겨야 한다.
 */

export type ChoreDifficulty = 'EASY' | 'NORMAL' | 'HARD';
export type ChoreRepeatType = 'ONCE' | 'DAY' | 'WEEK' | 'MONTH';
export type ChoreCompletionStatus = 'PENDING' | 'CANCELLED' | 'SUCCEED' | 'REJECTED';

/** GET .../chores 목록의 한 건. 활성(현재 기간에 해당하는) 집안일만 온다 */
export type ChoreSummary = {
  choreId: number;
  childId: number;
  title: string;
  difficulty: ChoreDifficulty;
  /** AI 산정 전이면 null (= 보상 산정 중) */
  rewardPoints: number | null;
  repeatType: ChoreRepeatType;
  startDate: string;
  endDate: string;
  createdAt: string;
};

/** GET .../completion-requests 목록의 한 건 (부모 승인 대기함과 같은 모양) */
export type ChoreCompletionSummary = {
  choreCompletionRequestId: number;
  choreId: number;
  choreTitle: string;
  childName: string;
  photoUrl: string;
  description: string | null;
  status: ChoreCompletionStatus;
  requestedAt: string;
};

/** GET .../completions 목록의 한 건. 승인·지급까지 끝난 것만 온다 */
export type ChoreCompletionHistoryItem = {
  choreId: number;
  choreTitle: string;
  childName: string;
  rewardPoints: number;
  processedAt: string;
};
