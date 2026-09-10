import { useMutation } from '@tanstack/react-query';

import { authApi } from '@/features/auth/api/authApi';

/**
 * 아이디 찾기 mutation. `POST /api/v1/auth/find-id`.
 * 이메일 인증에서 받은 verificationToken 이 필요하며, 응답의 loginId 는
 * 서버가 마스킹한 값이다 (예: `doto***23`).
 */
export function useFindId() {
  return useMutation({
    mutationFn: (body: { name: string; email: string; verificationToken: string }) =>
      authApi.findId(body),
  });
}

/**
 * 비밀번호 재설정 mutation. `POST /api/v1/auth/password-reset`.
 * 성공하면 204 이고 기존 세션은 모두 무효화되므로, 화면에서 로그인으로 보낸다.
 */
export function useResetPassword() {
  return useMutation({
    mutationFn: (body: {
      loginId: string;
      email: string;
      verificationToken: string;
      newPassword: string;
    }) => authApi.resetPassword(body),
  });
}
