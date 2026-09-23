import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import type { ReactElement } from 'react';

import { choreApi } from '@/features/chore/api/choreApi';
import type { ChoreCompletionHistoryItem, ChoreSummary } from '@/features/chore/model/types';

import { ChildChoreList } from './ChildChoreList';

jest.mock('@/features/chore/api/choreApi', () => ({
  choreApi: {
    getChores: jest.fn(),
    getCompletionRequests: jest.fn(),
    getCompletions: jest.fn(),
  },
}));

// 이 화면은 useChoreRefresh 를 통해 useFocusEffect 를 쓰는데, 그러려면 네비게이션
// 컨테이너가 있어야 한다. 화면을 감싸는 것보다 훅을 비워 두는 편이 낫다 - 여기서 보려는
// 것은 포커스 갱신이 아니라 목록에 무엇이 남는지다.
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
    // 주기 시작(START 자정) 이후라 이번 주기의 지급으로 센다
    processedAt: `${START}T12:00:00+09:00`,
  };
}

function renderList(): ReactElement {
  return <ChildChoreList familyId={FAMILY_ID} childId={CHILD_ID} />;
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

describe('ChildChoreList - 완료한 미션 처리', () => {
  it('끝낸 한번 미션은 목록에서 빠진다', async () => {
    // 다음 주기가 없어 다시 할 일이 아니다. 남겨 두면 "해야 할 미션" 자리에 영영 쌓인다.
    mocked.getChores.mockResolvedValue([chore(1, '수학 문제 20개 풀기'), chore(2, '방 정리하기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '수학 문제 20개 풀기')]);

    await withQuery(renderList());

    await waitFor(() => expect(screen.getByText('방 정리하기')).toBeTruthy(), WAIT);
    expect(screen.queryByText('수학 문제 20개 풀기')).toBeNull();
  });

  it('남은 개수만 배너에 센다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '수학 문제 20개 풀기'), chore(2, '방 정리하기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '수학 문제 20개 풀기')]);

    await withQuery(renderList());

    // 등록된 것은 둘이지만 하나는 이미 받았으므로 "1개 남았어요" 여야 한다.
    await waitFor(() => expect(screen.getByText(/1개 남았어요/)).toBeTruthy(), WAIT);
  });

  it('보상 합계에서도 받은 미션을 뺀다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '수학 문제 20개 풀기'), chore(2, '방 정리하기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '수학 문제 20개 풀기')]);

    await withQuery(renderList());

    // 남은 한 건의 500 만 센다. 1,000 이 뜨면 이미 받은 몫을 또 약속하는 셈이다.
    await waitFor(() => expect(screen.getByText('다 하면 + 500 도토리')).toBeTruthy(), WAIT);
  });

  it('전부 끝냈으면 미션이 없다고 하지 않는다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '수학 문제 20개 풀기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '수학 문제 20개 풀기')]);

    await withQuery(renderList());

    await waitFor(() => expect(screen.getByText(/미션을 모두 해냈어요/)).toBeTruthy(), WAIT);
    expect(screen.queryByText(/아직 미션이 없어요/)).toBeNull();
  });

  it('등록된 미션이 아예 없으면 부모님을 기다리라고 안내한다', async () => {
    mocked.getChores.mockResolvedValue([]);

    await withQuery(renderList());

    await waitFor(() => expect(screen.getByText(/아직 미션이 없어요/)).toBeTruthy(), WAIT);
  });

  it('지난 주기의 지급은 완료로 보지 않는다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '수학 문제 20개 풀기')]);
    // 주기 시작 직전의 지급 - 크론이 startDate 를 밀었으므로 이번 주기 것이 아니다.
    mocked.getCompletions.mockResolvedValue([
      { ...paid(1, '수학 문제 20개 풀기'), processedAt: `2026-09-14T23:59:00+09:00` },
    ]);

    await withQuery(renderList());

    await waitFor(() => expect(screen.getByText('수학 문제 20개 풀기')).toBeTruthy(), WAIT);
  });
});

describe('ChildChoreList - 알림에서 들어온 미션', () => {
  const push = jest.requireMock('expo-router').router.push as jest.Mock;

  it('이번 주기에 이미 받은 미션(승인 알림)이면 완료 이력으로 보낸다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '방 정리하기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '방 정리하기')]);
    const onConsumed = jest.fn();

    await withQuery(
      <ChildChoreList
        familyId={FAMILY_ID}
        childId={CHILD_ID}
        requestedChoreId={1}
        onRequestedChoreConsumed={onConsumed}
      />,
    );

    await waitFor(() => expect(push).toHaveBeenCalledWith('/task/history'), WAIT);
    expect(onConsumed).toHaveBeenCalledTimes(1);
  });

  it('목록에 없는 미션이어도 완료 이력으로 보낸다 - 할 일 폼을 열지 않는다', async () => {
    mocked.getChores.mockResolvedValue([chore(2, '설거지하기')]);

    await withQuery(
      <ChildChoreList familyId={FAMILY_ID} childId={CHILD_ID} requestedChoreId={1} />,
    );

    await waitFor(() => expect(push).toHaveBeenCalledWith('/task/history'), WAIT);
  });

  it('아직 할 미션(새 미션 알림)이면 완료 이력으로 보내지 않는다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '방 정리하기')]);
    const onConsumed = jest.fn();

    await withQuery(
      <ChildChoreList
        familyId={FAMILY_ID}
        childId={CHILD_ID}
        requestedChoreId={1}
        onRequestedChoreConsumed={onConsumed}
      />,
    );

    await waitFor(() => expect(onConsumed).toHaveBeenCalledTimes(1), WAIT);
    expect(await screen.findByText('방 정리하기', {}, WAIT)).toBeTruthy();
    expect(push).not.toHaveBeenCalledWith('/task/history');
  });
});

describe('ChildChoreList - 끝낸 반복 미션', () => {
  it('목록에 남는다', async () => {
    // 사라지면 아이도 부모도 무엇을 걸어 놨는지 알 수 없다 (QA). 흐리게 남기고, 상세에서
    // 완료 요청 버튼만 빠진다.
    mocked.getChores.mockResolvedValue([chore(1, '이불 개기', 'DAY')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '이불 개기')]);

    await withQuery(renderList());

    await waitFor(() => expect(screen.getByText('이불 개기')).toBeTruthy(), WAIT);
  });

  it('"미완료된 미션 보기" 를 켜면 빠진다', async () => {
    mocked.getChores.mockResolvedValue([chore(1, '이불 개기', 'DAY'), chore(2, '방 정리하기')]);
    mocked.getCompletions.mockResolvedValue([paid(1, '이불 개기')]);

    await withQuery(renderList());
    await waitFor(() => expect(screen.getByText('이불 개기')).toBeTruthy(), WAIT);

    // Checkbox(Pressable)에서 Toggle(RN Switch)로 바뀌어 press 대신 valueChange 를 쏜다
    fireEvent(screen.getByLabelText('미완료된 미션 보기'), 'valueChange', true);

    await waitFor(() => expect(screen.queryByText('이불 개기')).toBeNull(), WAIT);
    expect(screen.getByText('방 정리하기')).toBeTruthy();
  });
});
