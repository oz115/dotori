import { httpClient } from '@/shared/api';
import type { QuizParentToday, QuizSubmitResult, QuizToday } from '@/features/quiz/model/types';

const base = (familyId: number, childId: number) =>
  `/api/v1/families/${familyId}/children/${childId}/quizzes`;

/** 퀴즈 원시 HTTP 호출. 세션·토큰은 httpClient 인터셉터가, 캐시 갱신은 model 훅이 담당. */
export const quizApi = {
  /** 오늘의 퀴즈. 아직 생성 전이면 서버가 404(QUIZ_NOT_READY)를 준다 */
  getTodayQuiz: (familyId: number, childId: number) =>
    httpClient.get<QuizToday>(`${base(familyId, childId)}/today`).then((res) => res.data),

  /**
   * 부모용 오늘의 퀴즈 확인 — 정답과 아이의 제출 내역을 함께 준다. 부모만 부를 수 있고
   * (아이는 403), 아직 생성 전이면 404(QUIZ_NOT_READY)다.
   */
  getParentTodayQuiz: (familyId: number, childId: number) =>
    httpClient
      .get<QuizParentToday>(`${base(familyId, childId)}/today/review`)
      .then((res) => res.data),

  /** 답안 제출. 최대 3회, 정답이면 즉시 종료 + 포인트 지급 */
  submitQuiz: (familyId: number, childId: number, quizId: number, answer: string) =>
    httpClient
      .post<QuizSubmitResult>(`${base(familyId, childId)}/${quizId}/submit`, { answer })
      .then((res) => res.data),
};
