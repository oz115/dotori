import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { quizApi } from '@/features/quiz/api/quizApi';
import type { QuizSubmitResult, QuizToday } from '@/features/quiz/model/types';
import { QuizScreen } from './QuizScreen';

jest.mock('@/features/quiz/api/quizApi', () => ({
  quizApi: { getTodayQuiz: jest.fn(), submitQuiz: jest.fn() },
}));

const quiz = (overrides: Partial<QuizToday> = {}): QuizToday => ({
  quizId: 7,
  quizDate: '2026-09-21',
  question: "'이자'란 무엇일까요?",
  choices: null,
  rewardPoints: 60,
  alreadySubmitted: false,
  correctAnswer: null,
  explanation: null,
  attemptCount: 0,
  hint: null,
  ...overrides,
});

const submitResult = (overrides: Partial<QuizSubmitResult> = {}): QuizSubmitResult => ({
  quizAttemptId: 1,
  isCorrect: false,
  correctAnswer: null,
  explanation: null,
  earnedPoints: 0,
  pointBalanceAfter: 5000,
  hint: null,
  ...overrides,
});

async function show() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <QuizScreen familyId={1} childId={2} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
});

it('아직 한 번도 안 풀었으면 초성 힌트를 보여 주지 않는다', async () => {
  jest.mocked(quizApi.getTodayQuiz).mockResolvedValue(quiz());
  await show();

  expect(await screen.findByText("Q. '이자'란 무엇일까요?")).toBeTruthy();
  expect(screen.queryByText('초성 힌트')).toBeNull();
});

it('오답을 내면 서버가 준 초성 힌트를 보여 준다', async () => {
  jest.mocked(quizApi.getTodayQuiz).mockResolvedValue(quiz());
  jest.mocked(quizApi.submitQuiz).mockResolvedValue(submitResult({ hint: 'ㅇㅈ' }));
  await show();

  await fireEvent.changeText(await screen.findByPlaceholderText('정답을 입력해보세요'), '수수료');
  await fireEvent.press(screen.getByText('정답 확인'));

  expect(await screen.findByText('ㅇㅈ')).toBeTruthy();
  expect(screen.getByText('초성 힌트')).toBeTruthy();
  expect(quizApi.submitQuiz).toHaveBeenCalledWith(1, 2, 7, '수수료');
  // 힌트는 초성일 뿐 정답 자체는 아직 공개되지 않는다
  expect(screen.queryByText('이자')).toBeNull();
});

it('화면을 나갔다 다시 들어와도 오늘의 퀴즈 응답의 힌트를 보여 준다', async () => {
  jest.mocked(quizApi.getTodayQuiz).mockResolvedValue(quiz({ attemptCount: 1, hint: 'ㅇㅈ' }));
  await show();

  expect(await screen.findByText('ㅇㅈ')).toBeTruthy();
  expect(screen.getByText('초성 힌트')).toBeTruthy();
  expect(screen.getByText('도전 기회 2/3')).toBeTruthy();
});

it('정답이 공개된 상태에서는 힌트 대신 결과 화면을 보여 준다', async () => {
  jest.mocked(quizApi.getTodayQuiz).mockResolvedValue(
    quiz({
      attemptCount: 3,
      alreadySubmitted: true,
      correctAnswer: '이자',
      explanation: '돈을 맡기거나 빌려줘서 받는 보상이에요.',
      hint: null,
    }),
  );
  await show();

  expect(await screen.findByText(/돈을 맡기거나 빌려줘서 받는 보상이에요/)).toBeTruthy();
  expect(screen.queryByText('초성 힌트')).toBeNull();
});

it('정답을 맞혀도 결과 화면 버튼은 포인트 받기가 아니라 확인이다', async () => {
  jest.mocked(quizApi.getTodayQuiz).mockResolvedValue(quiz());
  jest.mocked(quizApi.submitQuiz).mockResolvedValue(
    submitResult({
      isCorrect: true,
      correctAnswer: '이자',
      explanation: '돈을 맡기거나 빌려줘서 받는 보상이에요.',
      earnedPoints: 60,
    }),
  );
  await show();

  await fireEvent.changeText(await screen.findByPlaceholderText('정답을 입력해보세요'), '이자');
  await fireEvent.press(screen.getByText('정답 확인'));

  // 포인트는 제출 시점에 이미 들어왔으니 버튼이 받을 게 남은 것처럼 보이면 안 된다
  expect(await screen.findByText('확인')).toBeTruthy();
  expect(screen.queryByText('포인트 받기')).toBeNull();
  expect(screen.getByText('+60P를 받았어요')).toBeTruthy();
});

it('정답 입력은 서버 컬럼 길이(255자)까지만 받는다', async () => {
  jest.mocked(quizApi.getTodayQuiz).mockResolvedValue(quiz());
  await show();

  expect(await screen.findByPlaceholderText('정답을 입력해보세요')).toHaveProp('maxLength', 255);
});
