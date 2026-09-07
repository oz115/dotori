import type { AxiosError } from 'axios';

/** 회원가입 실패 원인 → 사용자 문구. */
export function signupErrorMessage(error: unknown): string {
  const status = (error as AxiosError)?.response?.status;
  if (status === 409) {
    return '이미 사용 중인 아이디 또는 이메일이에요.';
  }
  if (status === 400) {
    return '입력한 정보를 다시 확인해주세요.';
  }
  return '회원가입에 실패했어요. 잠시 후 다시 시도해주세요.';
}
