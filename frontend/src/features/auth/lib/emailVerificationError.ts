import { apiErrorCode, isNetworkError } from '@/features/auth/lib/apiError';

/** 이메일 인증(코드 발송·확인) 실패 원인 → 사용자 문구. */
export function emailVerificationErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INVALID_CODE':
      return '인증 코드가 일치하지 않거나 만료됐어요.';
    case 'DUPLICATE_EMAIL':
      return '이미 가입된 이메일이에요.';
    case 'TOO_MANY_REQUESTS':
      return '너무 자주 요청했어요. 잠시 후 다시 시도해주세요.';
    default:
      return '인증에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
