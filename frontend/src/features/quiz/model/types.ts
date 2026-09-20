/**
 * GET .../quizzes/today 응답. `choices`는 AI가 항상 주관식으로만 만들어서 늘 null이다 —
 * 명세 필드라 남겨두되 화면에서 쓰지 않는다.
 *
 * `correctAnswer`·`explanation`은 `alreadySubmitted`가 true일 때만(=이미 정답을 맞혔거나
 * 3회 다 써서 더 제출할 수 없을 때만) 채워진다.
 *
 * `hint`는 한 번이라도 틀렸고 아직 재도전할 수 있을 때만 채워지는 정답의 초성이다(예: "ㅇㅈ").
 * 정답은 잠기기 전까지 내려오지 않으므로 서버가 만들어 준다. 그 외에는 null.
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
  hint: string | null;
};

/**
 * POST .../quizzes/{quizId}/submit 응답. `correctAnswer`·`explanation`은 정답을 맞혔거나
 * 이번이 3번째(마지막) 시도일 때만 채워진다 — 그 전 오답에서는 둘 다 null이라 재도전을
 * 유도한다.
 *
 * `hint`는 오답이고 아직 재도전할 수 있을 때만 정답의 초성이 채워진다.
 */
export type QuizSubmitResult = {
  quizAttemptId: number;
  isCorrect: boolean;
  correctAnswer: string | null;
  explanation: string | null;
  earnedPoints: number;
  pointBalanceAfter: number;
  hint: string | null;
};

/** 부모용 오늘의 퀴즈 확인 안의 시도 1회 (아이가 실제로 낸 답과 정오답). */
export type QuizParentAttempt = {
  quizAttemptId: number;
  /** 1부터 세는 제출 순서 */
  attemptNumber: number;
  submittedAnswer: string;
  isCorrect: boolean;
  earnedPoints: number;
  submittedAt: string;
};

/**
 * GET .../quizzes/today/review 응답 — **부모 전용**. 아이가 아직 안 풀었어도 정답을 준다.
 * 아이용 QuizToday 와 달리 정답이 잠금 여부와 무관하게 항상 채워지므로, 이 타입은
 * 아이 화면 쪽 코드에서 절대 쓰지 않는다.
 */
export type QuizParentToday = {
  quizId: number;
  quizDate: string;
  question: string;
  correctAnswer: string;
  explanation: string | null;
  attemptCount: number;
  /** 하루 최대 제출 횟수 (서버 규칙) */
  maxAttempts: number;
  solved: boolean;
  /** 정답을 맞혔거나 횟수를 다 써서 더 제출할 수 없는 상태 */
  locked: boolean;
  /** 제출 순서대로. 아직 안 풀었으면 빈 배열 */
  attempts: QuizParentAttempt[];
};
