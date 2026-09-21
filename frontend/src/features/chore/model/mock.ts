import type { ChoreCompletionSummary } from '@/features/chore/model/types';

const TODAY = new Date().toISOString().slice(0, 10);

/**
 * 화면 확인용 예시 승인 대기함 (부모 홈 "확인해주세요").
 *
 * 사진은 서버가 주는 presigned URL 이라 예시로 만들 수 없어 빈 문자열이다 —
 * 홈은 건수만 세고 사진은 할 일 탭에서 보므로 여기선 문제되지 않는다.
 */
export const mockPendingApprovals: ChoreCompletionSummary[] = [
  {
    choreCompletionRequestId: -1,
    choreId: -1,
    choreTitle: '설거지 하기',
    childName: '우리 아이',
    photoUrl: '',
    description: '다 했어요!',
    status: 'PENDING',
    requestedAt: `${TODAY}T18:20:00+09:00`,
  },
];
