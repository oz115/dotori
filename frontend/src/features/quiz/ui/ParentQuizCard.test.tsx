import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { quizApi } from '@/features/quiz/api/quizApi';
import type { QuizParentToday } from '@/features/quiz/model/types';
import { ParentQuizCard } from './ParentQuizCard';

jest.mock('@/features/quiz/api/quizApi', () => ({
  quizApi: { getParentTodayQuiz: jest.fn() },
}));
// 화면 포커스 훅은 네비게이션 컨테이너가 필요해서 테스트에선 no-op 으로 둔다 (다른 화면 테스트와 같다)
jest.mock('expo-router', () => ({
  ...jest.requireActual('expo-router'),
  useFocusEffect: () => {},
}));

const attempt = (n: number, answer: string, isCorrect: boolean) => ({
  quizAttemptId: n,
  attemptNumber: n,
  submittedAnswer: answer,
  isCorrect,
  earnedPoints: isCorrect ? 100 : 0,
  submittedAt: '2026-09-21T10:00:00+09:00',
});

const quiz = (overrides: Partial<QuizParentToday> = {}): QuizParentToday => ({
  quizId: 7,
  quizDate: '2026-09-21',
  question: "'인플레이션'이란 무엇일까요?",
  correctAnswer: '물가가 계속 오르는 현상',
  explanation: null,
  attemptCount: 0,
  maxAttempts: 3,
  solved: false,
  locked: false,
  attempts: [],
  ...overrides,
});

const apiError = (code: string, status: number) => ({
  response: { status, data: { code, message: '' } },
});

async function show(props: { familyId?: number; childId?: number; familyPending?: boolean } = {}) {
  // 훅이 retry 를 직접 정하므로 여기서는 재시도 간격만 없앤다
  const client = new QueryClient({ defaultOptions: { queries: { retryDelay: 0, gcTime: 0 } } });
  // 기본값 구조분해는 명시적 undefined 를 기본값으로 바꿔 버려서 "id 없음" 상태를 만들 수 없다
  const familyId = 'familyId' in props ? props.familyId : 1;
  const childId = 'childId' in props ? props.childId : 2;
  const familyPending = props.familyPending ?? false;
  return render(
    <QueryClientProvider client={client}>
      <ParentQuizCard familyId={familyId} childId={childId} familyPending={familyPending} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
});

it('아이가 아직 안 풀었으면 문제와 정답만 보여 준다', async () => {
  jest.mocked(quizApi.getParentTodayQuiz).mockResolvedValue(quiz());
  await show();

  expect(await screen.findByText("'인플레이션'이란 무엇일까요?")).toBeTruthy();
  expect(screen.getByText('물가가 계속 오르는 현상')).toBeTruthy();
  expect(screen.getByText('아직 안 풀었어요')).toBeTruthy();
  expect(screen.getByText('아이가 아직 풀지 않았어요')).toBeTruthy();
  expect(screen.queryByText(/아이가 제출한 답/)).toBeNull();
  expect(quizApi.getParentTodayQuiz).toHaveBeenCalledWith(1, 2);
});

it('틀린 답을 낸 아이의 제출 내역을 시도별로 보여 준다', async () => {
  jest.mocked(quizApi.getParentTodayQuiz).mockResolvedValue(
    quiz({
      attemptCount: 2,
      attempts: [attempt(1, '물가가 내려가는 것', false), attempt(2, '돈이 많아지는 것', false)],
    }),
  );
  await show();

  expect(await screen.findByText('아이가 제출한 답 (2/3회)')).toBeTruthy();
  expect(screen.getByText('물가가 내려가는 것')).toBeTruthy();
  expect(screen.getByText('돈이 많아지는 것')).toBeTruthy();
  expect(screen.getAllByText('오답')).toHaveLength(2);
  expect(screen.getByText('도전 중')).toBeTruthy();
  // 정답도 함께 보인다
  expect(screen.getByText('물가가 계속 오르는 현상')).toBeTruthy();
});

it('오답 뒤 정답을 맞힌 내역은 정답 상태로 보여 준다', async () => {
  jest.mocked(quizApi.getParentTodayQuiz).mockResolvedValue(
    quiz({
      attemptCount: 2,
      solved: true,
      locked: true,
      attempts: [attempt(1, '모르겠어요', false), attempt(2, '물가가 계속 오르는 현상', true)],
    }),
  );
  await show();

  expect(await screen.findByText('아이가 제출한 답 (2/3회)')).toBeTruthy();
  // 헤더 배지와 제출 줄의 "정답" 라벨, 그리고 필드 제목 "정답"
  expect(screen.getAllByText('정답').length).toBeGreaterThanOrEqual(2);
  expect(screen.getAllByText('오답')).toHaveLength(1);
});

it('기회를 다 쓰고도 못 맞힌 아이는 오답 상태로 보여 준다', async () => {
  jest.mocked(quizApi.getParentTodayQuiz).mockResolvedValue(
    quiz({
      attemptCount: 3,
      locked: true,
      attempts: [attempt(1, 'a', false), attempt(2, 'b', false), attempt(3, 'c', false)],
    }),
  );
  await show();

  expect(await screen.findByText('아이가 제출한 답 (3/3회)')).toBeTruthy();
  expect(screen.getAllByText('오답')).toHaveLength(4); // 배지 1 + 제출 줄 3
});

it('오늘의 퀴즈가 아직 생성 전이면 오류가 아니라 준비 중 안내를 보여 준다', async () => {
  jest.mocked(quizApi.getParentTodayQuiz).mockRejectedValue(apiError('QUIZ_NOT_READY', 404));
  await show();

  expect(await screen.findByText('오늘의 퀴즈가 아직 준비되지 않았어요')).toBeTruthy();
  expect(screen.queryByText('다시 시도')).toBeNull();
  expect(screen.queryByText(/불러오지 못했어요/)).toBeNull();
});

it('권한이 없으면 권한 안내를 보여 주고 재시도를 권하지 않는다', async () => {
  jest.mocked(quizApi.getParentTodayQuiz).mockRejectedValue(apiError('FORBIDDEN', 403));
  await show();

  expect(await screen.findByText('이 퀴즈를 볼 수 있는 권한이 없어요')).toBeTruthy();
  expect(screen.queryByText('다시 시도')).toBeNull();
});

it('서버 오류면 오류 문구와 다시 시도 버튼을 보여 주고, 누르면 다시 조회한다', async () => {
  // 훅이 일반 오류는 한 번 자동 재시도하므로 실패를 두 번 준비한다
  jest
    .mocked(quizApi.getParentTodayQuiz)
    .mockRejectedValueOnce(apiError('INTERNAL_SERVER_ERROR', 500))
    .mockRejectedValueOnce(apiError('INTERNAL_SERVER_ERROR', 500))
    .mockResolvedValueOnce(quiz());
  await show();

  expect(await screen.findByText(/퀴즈를 불러오지 못했어요/)).toBeTruthy();
  fireEvent.press(screen.getByText('다시 시도'));

  expect(await screen.findByText('물가가 계속 오르는 현상')).toBeTruthy();
  expect(quizApi.getParentTodayQuiz).toHaveBeenCalledTimes(3);
});

it('가족 조회가 끝나기 전에는 불러오는 중으로 두고 API 를 부르지 않는다', async () => {
  await show({ familyId: undefined, childId: undefined, familyPending: true });

  expect(screen.getByText('불러오는 중이에요')).toBeTruthy();
  expect(quizApi.getParentTodayQuiz).not.toHaveBeenCalled();
});

it('연결된 자녀가 없으면 안내만 보여 주고 API 를 부르지 않는다', async () => {
  await show({ childId: undefined });

  expect(screen.getByText('연결된 자녀가 없어요')).toBeTruthy();
  expect(quizApi.getParentTodayQuiz).not.toHaveBeenCalled();
});
