import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react-native';

import { choreApi } from '@/features/chore/api/choreApi';
import { todayDate } from '@/features/chore';
import type { ChoreCompletionHistoryItem, ChoreSummary } from '@/features/chore/model/types';

import { TodayChores } from './TodayChores';

jest.mock('@/features/chore/api/choreApi', () => ({
  choreApi: {
    getChores: jest.fn(),
    getCompletionRequests: jest.fn(),
    getCompletions: jest.fn(),
  },
}));

// useChoreRefresh 가 useFocusEffect 를 쓴다 — 네비게이션 컨테이너 대신 훅을 비워 둔다
// (ChildChoreList 테스트와 같은 이유).
jest.mock('expo-router', () => ({
  router: { push: jest.fn(), back: jest.fn(), canGoBack: () => false, replace: jest.fn() },
  useFocusEffect: () => {},
}));

// chores 응답이 온 뒤에야 completions 를 묻는 연쇄 구조라 기본 1초로는 모자랄 때가 있다.
const WAIT = { timeout: 5000 };

const mocked = choreApi as jest.Mocked<typeof choreApi>;

const FAMILY_ID = 1;
const CHILD_ID = 7;
const TODAY = todayDate();

function chore(choreId: number, title: string): ChoreSummary {
  return {
    choreId,
    childId: CHILD_ID,
    title,
    difficulty: 'NORMAL',
    rewardPoints: 500,
    repeatType: 'ONCE',
    startDate: TODAY,
    endDate: TODAY,
    createdAt: `${TODAY}T09:00:00+09:00`,
  };
}

function paid(choreId: number, choreTitle: string): ChoreCompletionHistoryItem {
  return {
    choreId,
    choreTitle,
    childName: '민준',
    rewardPoints: 500,
    processedAt: `${TODAY}T12:00:00+09:00`,
  };
}

function show() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <TodayChores familyId={FAMILY_ID} childId={CHILD_ID} onMorePress={() => {}} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  mocked.getCompletionRequests.mockResolvedValue([]);
  mocked.getCompletions.mockResolvedValue([]);
});

it('오늘 등록된 미션을 다 끝냈으면 없다고 하지 않는다', async () => {
  mocked.getChores.mockResolvedValue([chore(1, '방 정리하기')]);
  mocked.getCompletions.mockResolvedValue([paid(1, '방 정리하기')]);

  await show();

  await waitFor(() => expect(screen.getByText(/모두 완료했어요/)).toBeTruthy(), WAIT);
  expect(screen.queryByText('오늘 등록된 미션이 없어요.')).toBeNull();
});

it('오늘 등록된 미션이 아예 없으면 없다고 알린다', async () => {
  mocked.getChores.mockResolvedValue([]);

  await show();

  await waitFor(() => expect(screen.getByText('오늘 등록된 미션이 없어요.')).toBeTruthy(), WAIT);
});
