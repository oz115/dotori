import { apiErrorCode, isNetworkError } from '@/features/auth/lib/apiError';

/** 회원가입 실패 원인 → 사용자 문구. */
export function signupErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'DUPLICATE_LOGIN_ID':
      return '이미 사용 중인 아이디예요.';
    case 'DUPLICATE_EMAIL':
      return '이미 사용 중인 이메일이에요.';
    case 'INVALID_VERIFICATION_TOKEN':
      return '이메일 인증이 만료됐어요. 이전 단계에서 다시 인증해주세요.';
    case 'INVALID_PASSWORD_FORMAT':
      return '비밀번호 형식을 확인해주세요.';
    default:
      return '회원가입에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
