import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 오늘의 퀴즈 조회 실패 → 사용자 문구. QUIZ_NOT_READY는 에러가 아니라 화면이 따로 다룬다. */
export function quizTodayErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  return '퀴즈를 불러오지 못했어요. 잠시 후 다시 시도해주세요.';
}

/**
 * 부모용 오늘의 퀴즈 조회 실패의 종류. NOT_READY 는 오류가 아니라 "아직 준비 전" 안내로,
 * FORBIDDEN 은 다시 시도해도 소용없는 권한 문제로 화면이 따로 다룬다.
 */
export type ParentQuizFailure = 'NOT_READY' | 'FORBIDDEN' | 'NETWORK' | 'ERROR';

export function parentQuizFailure(error: unknown): ParentQuizFailure {
  if (isNetworkError(error)) {
    return 'NETWORK';
  }
  switch (apiErrorCode(error)) {
    case 'QUIZ_NOT_READY':
      return 'NOT_READY';
    case 'FORBIDDEN':
      return 'FORBIDDEN';
    default:
      return 'ERROR';
  }
}

/** 부모용 오늘의 퀴즈 조회 실패 → 사용자 문구. */
export function parentQuizFailureMessage(failure: ParentQuizFailure): string {
  switch (failure) {
    case 'NOT_READY':
      return '오늘의 퀴즈가 아직 준비되지 않았어요';
    case 'FORBIDDEN':
      return '이 퀴즈를 볼 수 있는 권한이 없어요';
    case 'NETWORK':
      return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
    case 'ERROR':
      return '퀴즈를 불러오지 못했어요. 잠시 후 다시 시도해주세요.';
  }
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
