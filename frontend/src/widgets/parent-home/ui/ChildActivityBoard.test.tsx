import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { httpClient } from '@/shared/api';
import { ChildActivityBoard } from './ChildActivityBoard';

// 다른 슬라이스의 api 파일이 아니라 공용 HTTP 클라이언트를 막는다 — 위젯은 슬라이스 내부를 모른다
jest.mock('@/shared/api', () => ({
  ...jest.requireActual('@/shared/api'),
  httpClient: { get: jest.fn() },
}));
// 화면 포커스 훅은 네비게이션 컨테이너가 필요해서 테스트에선 no-op 으로 둔다 (다른 화면 테스트와 같다)
jest.mock('expo-router', () => ({
  ...jest.requireActual('expo-router'),
  useFocusEffect: () => {},
}));

const onOpenQuiz = jest.fn();
const onOpenWords = jest.fn();

async function show(props: { childId?: number } = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <ChildActivityBoard
        familyId={1}
        childId={'childId' in props ? props.childId : 2}
        onOpenQuiz={onOpenQuiz}
        onOpenWords={onOpenWords}
      />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  jest.mocked(httpClient.get).mockResolvedValue({
    data: {
      quizId: 7,
      quizDate: '2026-09-21',
      question: "'이자'란 무엇일까요?",
      correctAnswer: '돈을 맡기고 받는 보상',
      explanation: null,
      attemptCount: 0,
      maxAttempts: 3,
      solved: false,
      locked: false,
      attempts: [],
    },
  });
});

it('퀴즈 카드는 홈에서 문제·정답을 펼치지 않고, 아이가 풀었는지 배지만 보여 준다', async () => {
  await show();

  expect(await screen.findByText('아직 안 풀었어요')).toBeTruthy();
  expect(httpClient.get).toHaveBeenCalledWith('/api/v1/families/1/children/2/quizzes/today/review');
  // 문제·정답은 전용 화면에서만 보인다
  expect(screen.queryByText("'이자'란 무엇일까요?")).toBeNull();
  expect(screen.queryByText('돈을 맡기고 받는 보상')).toBeNull();
  // 예전 하드코딩 값이 남아 있지 않다
  expect(screen.queryByText('예시')).toBeNull();
  expect(screen.queryByText('오늘의 단어: 중앙은행')).toBeNull();
});

it('퀴즈 카드를 누르면 퀴즈 화면으로 보낸다', async () => {
  await show();

  fireEvent.press(screen.getByLabelText('오늘의 경제 퀴즈 열기'));

  expect(onOpenQuiz).toHaveBeenCalledTimes(1);
});

it('단어장 카드를 누르면 단어장 화면으로 보낸다', async () => {
  await show();

  fireEvent.press(screen.getByLabelText('경제 단어장 열기'));

  expect(onOpenWords).toHaveBeenCalledTimes(1);
});

it('자녀가 없으면 퀴즈를 조회하지 않고 배지도 없지만 카드는 누를 수 있다', async () => {
  await show({ childId: undefined });

  expect(httpClient.get).not.toHaveBeenCalled();
  expect(screen.queryByText('아직 안 풀었어요')).toBeNull();
  fireEvent.press(screen.getByLabelText('오늘의 경제 퀴즈 열기'));

  expect(onOpenQuiz).toHaveBeenCalledTimes(1);
});

it('퀴즈 조회가 실패해도 카드는 눌러서 들어갈 수 있다', async () => {
  jest.mocked(httpClient.get).mockRejectedValue({
    response: { status: 404, data: { code: 'QUIZ_NOT_READY', message: '' } },
  });
  await show();

  fireEvent.press(screen.getByLabelText('오늘의 경제 퀴즈 열기'));

  expect(onOpenQuiz).toHaveBeenCalledTimes(1);
  expect(screen.queryByText('아직 안 풀었어요')).toBeNull();
});
