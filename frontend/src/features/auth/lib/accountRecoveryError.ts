import { apiErrorCode, isNetworkError } from '@/features/auth/lib/apiError';

/** 아이디 찾기 실패 원인 → 사용자 문구. */
export function findIdErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'USER_NOT_FOUND':
      return '입력한 이름과 이메일로 가입된 계정을 찾을 수 없어요.';
    case 'INVALID_VERIFICATION_TOKEN':
      return '이메일 인증이 만료됐어요. 인증을 다시 진행해주세요.';
    default:
      return '아이디 찾기에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}

/** 비밀번호 재설정 실패 원인 → 사용자 문구. */
export function resetPasswordErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '서버에 연결할 수 없어요. 네트워크를 확인해주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'USER_NOT_FOUND':
      return '입력한 아이디와 이메일이 일치하는 계정을 찾을 수 없어요.';
    case 'INVALID_VERIFICATION_TOKEN':
      return '이메일 인증이 만료됐어요. 인증을 다시 진행해주세요.';
    case 'INVALID_PASSWORD_FORMAT':
      return '비밀번호는 공백 없이 8자 이상 64자 이하여야 해요.';
    default:
      return '비밀번호 재설정에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
