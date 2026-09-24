import type { QuizParentToday } from '@/features/quiz/model/types';

/**
 * 부모 홈 "오늘의 경제 퀴즈" 카드가 아이의 진행 상황을 어떻게 읽을지 정한다.
 *
 * 서버가 solved·locked 를 이미 계산해 주므로 시도 횟수(3회)를 여기서 다시 세지 않는다 —
 * 하루 최대 횟수가 바뀌어도 이 파일은 그대로다.
 */

/**
 * - UNSOLVED: 아직 한 번도 제출하지 않음
 * - IN_PROGRESS: 오답을 냈지만 기회가 남아 다시 도전 중
 * - SOLVED: 정답을 맞힘 (몇 번째 시도든)
 * - FAILED: 기회를 다 쓰고도 못 맞힘
 */
export type ParentQuizStatus = 'UNSOLVED' | 'IN_PROGRESS' | 'SOLVED' | 'FAILED';

export function parentQuizStatus(
  quiz: Pick<QuizParentToday, 'solved' | 'locked' | 'attempts'>,
): ParentQuizStatus {
  if (quiz.solved) {
    return 'SOLVED';
  }
  if (quiz.attempts.length === 0) {
    return 'UNSOLVED';
  }
  return quiz.locked ? 'FAILED' : 'IN_PROGRESS';
}

export function parentQuizStatusLabel(status: ParentQuizStatus): string {
  switch (status) {
    case 'UNSOLVED':
      return '아직 안 풀었어요';
    case 'IN_PROGRESS':
      return '도전 중';
    case 'SOLVED':
      return '정답';
    case 'FAILED':
      return '오답';
  }
}

/**
 * 제출 내역을 "아이가 낸 답" 줄로 보여줄지. 안 푼 카드에 빈 목록 제목만 남지 않게,
 * 제출이 한 건이라도 있을 때만 그 구역을 그린다.
 */
export function hasSubmissions(quiz: Pick<QuizParentToday, 'attempts'>): boolean {
  return quiz.attempts.length > 0;
}
