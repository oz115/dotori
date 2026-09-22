import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import type { ReactElement } from 'react';

import { choreApi } from '@/features/chore/api/choreApi';
import type { ChoreCompletionHistoryItem, ChoreSummary } from '@/features/chore/model/types';

import { ParentChoreList } from './ParentChoreList';

jest.mock('@/features/chore/api/choreApi', () => ({
  choreApi: {
    getChores: jest.fn(),
    getCompletionRequests: jest.fn(),
    getCompletions: jest.fn(),
  },
}));

jest.mock('expo-router', () => ({
  router: { push: jest.fn(), back: jest.fn(), canGoBack: () => false, replace: jest.fn() },
  useFocusEffect: () => {},
}));

// 이 화면은 chores 응답이 온 뒤에야 completions 를 묻는 연쇄 구조라, 기본 1초로는
// 콜드 스타트에서 간헐적으로 모자란다.
const WAIT = { timeout: 5000 };

const mocked = choreApi as jest.Mocked<typeof choreApi>;

const FAMILY_ID = 1;
const CHILD_ID = 7;
const START = '2026-09-15';
const CHILDREN = [{ childId: CHILD_ID, name: '민준' }];

function chore(
  choreId: number,
  title: string,
  repeatType: ChoreSummary['repeatType'] = 'ONCE',
): ChoreSummary {
  return {
    choreId,
    childId: CHILD_ID,
    title,
    difficulty: 'NORMAL',
    rewardPoints: 500,
    repeatType,
    startDate: START,
    endDate: START,
    createdAt: `${START}T09:00:00+09:00`,
  };
}

function paid(choreId: number, choreTitle: string): ChoreCompletionHistoryItem {
  return {
    choreId,
    choreTitle,
    childName: '민준',
    rewardPoints: 500,
    processedAt: `${START}T12:00:00+09:00`,
  };
}

function withQuery(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

beforeEach(() => {
  jest.clearAllMocks();
  mocked.getCompletionRequests.mockResolvedValue([]);
  mocked.getCompletions.mockResolvedValue([]);
});

describe('ParentChoreList - 완료한 미션 처리', () => {
  it('끝낸 한번 미션은 목록에서 빠진다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '수학 문제 20개 풀기'), chore(2, '방 정리하기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '수학 문제 20개 풀기')]);

    await withQuery(<ParentChoreList familyId={FAMILY_ID} childList={CHILDREN} />);

    await waitFor(() => expect(screen.getByText('방 정리하기')).toBeTruthy(), WAIT);
    expect(screen.queryByText('수학 문제 20개 풀기')).toBeNull();
  });

  it('전부 끝냈으면 등록한 미션이 없다고 하지 않는다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '수학 문제 20개 풀기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '수학 문제 20개 풀기')]);

    await withQuery(<ParentChoreList familyId={FAMILY_ID} childList={CHILDREN} />);

    await waitFor(() => expect(screen.getByText(/미션을 모두 끝냈어요/)).toBeTruthy(), WAIT);
    expect(screen.queryByText(/아직 등록한 미션이 없어요/)).toBeNull();
  });

  it('등록한 것이 아예 없으면 만들라고 안내한다', async () => {
    mocked.getChores.mockResolvedValue([]);

    await withQuery(<ParentChoreList familyId={FAMILY_ID} childList={CHILDREN} />);

    await waitFor(() => expect(screen.getByText(/아직 등록한 미션이 없어요/)).toBeTruthy(), WAIT);
  });

  it('지난 주기의 지급은 완료로 보지 않는다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '수학 문제 20개 풀기')]);
    mocked.getCompletions.mockResolvedValue([
      { ...paid(1, '수학 문제 20개 풀기'), processedAt: '2026-09-14T23:59:00+09:00' },
    ]);

    await withQuery(<ParentChoreList familyId={FAMILY_ID} childList={CHILDREN} />);

    await waitFor(() => expect(screen.getByText('수학 문제 20개 풀기')).toBeTruthy(), WAIT);
  });

  it('한 아이의 지급이 다른 아이의 미션을 지우지 않는다', async () => {
    // 자녀별로 따로 조회하므로 완료 내역이 섞이면 안 된다.
    mocked.getChores.mockImplementation((_familyId, childId) =>
      Promise.resolve([{ ...chore(childId, `${childId}번 미션`), childId }]),
    );
    mocked.getCompletions.mockImplementation((_familyId, childId) =>
      Promise.resolve(childId === 7 ? [paid(7, '7번 미션')] : []),
    );

    await withQuery(
      <ParentChoreList
        familyId={FAMILY_ID}
        childList={[
          { childId: 7, name: '민준' },
          { childId: 8, name: '서윤' },
        ]}
      />,
    );

    await waitFor(() => expect(screen.getByText('8번 미션')).toBeTruthy(), WAIT);
    expect(screen.queryByText('7번 미션')).toBeNull();
  });
});

describe('ParentChoreList - 끝낸 반복 미션', () => {
  it('목록에 남는다', async () => {
    // 부모는 자기가 무엇을 걸어 뒀는지 기억하지 못한다. 끝나자마자 사라지면 그 미션이
    // 있었다는 사실 자체가 안 보인다 (QA). 아이 화면과 같은 규칙이다.
    mocked.getChores.mockResolvedValue([chore(1, '이불 개기', 'DAY')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '이불 개기')]);

    await withQuery(<ParentChoreList familyId={FAMILY_ID} childList={CHILDREN} />);

    await waitFor(() => expect(screen.getByText('이불 개기')).toBeTruthy(), WAIT);
  });

  it('"미완료만" 을 켜면 빠진다', async () => {
    // 아이 화면과 같은 장치다. 남겨 두는 것이 기본이되, 쌓이면 한 번 눌러 걷어낸다.
    mocked.getChores.mockResolvedValue([chore(1, '이불 개기', 'DAY'), chore(2, '방 정리하기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '이불 개기')]);

    await withQuery(<ParentChoreList familyId={FAMILY_ID} childList={CHILDREN} />);
    await waitFor(() => expect(screen.getByText('이불 개기')).toBeTruthy(), WAIT);

    fireEvent.press(screen.getByLabelText('미완료만'));

    await waitFor(() => expect(screen.queryByText('이불 개기')).toBeNull(), WAIT);
    expect(screen.getByText('방 정리하기')).toBeTruthy();
  });
});
