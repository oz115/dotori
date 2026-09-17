/**
 * GET .../quizzes/today 응답. `choices`는 AI가 항상 주관식으로만 만들어서 늘 null이다 —
 * 명세 필드라 남겨두되 화면에서 쓰지 않는다.
 *
 * `correctAnswer`·`explanation`은 `alreadySubmitted`가 true일 때만(=이미 정답을 맞혔거나
 * 3회 다 써서 더 제출할 수 없을 때만) 채워진다.
 */
export type QuizToday = {
  quizId: number;
  quizDate: string;
  question: string;
  choices: string[] | null;
  rewardPoints: number;
  alreadySubmitted: boolean;
  correctAnswer: string | null;
  explanation: string | null;
  /** 지금까지 제출한 시도 수(0~3). 화면을 나갔다 다시 들어와도 "도전 기회 n/3" 표시가 맞도록 서버가 내려준다. */
  attemptCount: number;
};

/**
 * POST .../quizzes/{quizId}/submit 응답. `correctAnswer`·`explanation`은 정답을 맞혔거나
 * 이번이 3번째(마지막) 시도일 때만 채워진다 — 그 전 오답에서는 둘 다 null이라 재도전을
 * 유도한다.
 */
export type QuizSubmitResult = {
  quizAttemptId: number;
  isCorrect: boolean;
  correctAnswer: string | null;
  explanation: string | null;
  earnedPoints: number;
  pointBalanceAfter: number;
};
