import { hasSubmissions, parentQuizStatus, parentQuizStatusLabel } from './parentQuiz';

const attempt = (n: number, isCorrect: boolean) => ({
  quizAttemptId: n,
  attemptNumber: n,
  submittedAnswer: '답',
  isCorrect,
  earnedPoints: isCorrect ? 100 : 0,
  submittedAt: '2026-09-21T10:00:00+09:00',
});

describe('parentQuizStatus', () => {
  it('제출이 없으면 아직 안 푼 상태다', () => {
    const quiz = { solved: false, locked: false, attempts: [] };
    expect(parentQuizStatus(quiz)).toBe('UNSOLVED');
    expect(parentQuizStatusLabel('UNSOLVED')).toBe('아직 안 풀었어요');
    expect(hasSubmissions(quiz)).toBe(false);
  });

  it('첫 시도에 맞혔으면 정답이다', () => {
    const quiz = { solved: true, locked: true, attempts: [attempt(1, true)] };
    expect(parentQuizStatus(quiz)).toBe('SOLVED');
    expect(hasSubmissions(quiz)).toBe(true);
  });

  it('오답 뒤 정답을 맞혀도 정답이다', () => {
    const quiz = { solved: true, locked: true, attempts: [attempt(1, false), attempt(2, true)] };
    expect(parentQuizStatus(quiz)).toBe('SOLVED');
  });

  it('오답이지만 기회가 남았으면 도전 중이다', () => {
    const quiz = { solved: false, locked: false, attempts: [attempt(1, false)] };
    expect(parentQuizStatus(quiz)).toBe('IN_PROGRESS');
    expect(parentQuizStatusLabel('IN_PROGRESS')).toBe('도전 중');
  });

  it('기회를 다 쓰고도 못 맞히면 오답이다', () => {
    const quiz = {
      solved: false,
      locked: true,
      attempts: [attempt(1, false), attempt(2, false), attempt(3, false)],
    };
    expect(parentQuizStatus(quiz)).toBe('FAILED');
    expect(parentQuizStatusLabel('FAILED')).toBe('오답');
  });
});
