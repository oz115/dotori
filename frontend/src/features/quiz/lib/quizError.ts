import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 오늘의 퀴즈 조회 실패 → 사용자 문구. QUIZ_NOT_READY는 에러가 아니라 화면이 따로 다룬다. */
export function quizTodayErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  return '퀴즈를 불러오지 못했어요. 잠시 후 다시 시도해주세요.';
}

/** 퀴즈 제출 실패 → 사용자 문구. */
export function quizSubmitErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'ALREADY_SUBMITTED':
      return '이미 정답을 맞힌 퀴즈예요.';
    case 'QUIZ_ATTEMPT_LIMIT_EXCEEDED':
      return '오늘 제출 기회를 다 썼어요.';
    case 'QUIZ_NOT_FOUND':
      return '퀴즈를 찾을 수 없어요.';
    default:
      return '제출에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
