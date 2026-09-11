import type { ChoreCompletionSummary } from '@/features/chore/model/types';
import type { TodayChore } from '@/features/chore/model/useTodayChores';

/**
 * 화면 확인용 예시 할 일.
 *
 * 부모가 아직 집안일을 등록하지 않으면 자녀 홈이 텅 비어 배치를 볼 수 없어서,
 * 그럴 때만 자리에 채워 넣는다. 실제 데이터가 하나라도 있으면 쓰지 않는다.
 * (`features/investment/model/mock` 과 같은 용도 — 화면이 붙으면 지운다)
 */
const TODAY = new Date().toISOString().slice(0, 10);

export const mockTodayChores: TodayChore[] = [
  {
    choreId: -1,
    childId: -1,
    title: '설거지 하기',
    difficulty: 'EASY',
    rewardPoints: 500,
    repeatType: 'DAY',
    startDate: TODAY,
    endDate: TODAY,
    createdAt: `${TODAY}T09:00:00+09:00`,
    status: 'TODO',
  },
  {
    choreId: -2,
    childId: -1,
    title: '방 청소하기',
    difficulty: 'NORMAL',
    rewardPoints: 300,
    repeatType: 'DAY',
    startDate: TODAY,
    endDate: TODAY,
    createdAt: `${TODAY}T09:00:00+09:00`,
    status: 'PENDING',
  },
  {
    choreId: -3,
    childId: -1,
    title: '분리수거 하기',
    difficulty: 'EASY',
    rewardPoints: 200,
    repeatType: 'DAY',
    startDate: TODAY,
    endDate: TODAY,
    createdAt: `${TODAY}T09:00:00+09:00`,
    status: 'DONE',
  },
];

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
