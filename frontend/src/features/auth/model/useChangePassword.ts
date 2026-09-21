// Created: 2026-09-19 13:19:06
import { useMutation } from '@tanstack/react-query';

import { changePassword } from '@/features/auth/model/session';

/**
 * 마이페이지 - 비밀번호 변경 mutation.
 * 성공하면 `changePassword`가 로컬 세션까지 정리해 root 가드가 로그인 화면으로 되돌린다.
 */
export function useChangePassword() {
  return useMutation({
    mutationFn: (body: { currentPassword: string; newPassword: string }) =>
      changePassword(body.currentPassword, body.newPassword),
  });
}
