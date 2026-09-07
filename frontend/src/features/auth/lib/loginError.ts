import type { AxiosError } from 'axios';

/** 로그인 실패 원인을 사용자 문구로 바꾼다. */
export function loginErrorMessage(error: unknown): string {
  const status = (error as AxiosError)?.response?.status;
  if (status === 400 || status === 401) {
    return '아이디 또는 비밀번호를 확인해주세요.';
  }
  return '로그인에 실패했어요. 잠시 후 다시 시도해주세요.';
}
