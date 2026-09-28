// Created: 2026-09-19 13:19:06
import { useMutation } from '@tanstack/react-query';

import { changePassword } from '@/features/auth/model/session';

/**
 * 마이페이지 - 비밀번호 변경 mutation.
 *
 * 로컬 세션은 여기서 정리하지 않는다. 화면이 "다시 로그인해주세요" 안내를 보여 주고,
 * 사용자가 확인을 누른 뒤에 정리한다 (clearSessionAfterPasswordChange).
 */
export function useChangePassword() {
  return useMutation({
    mutationFn: (body: { currentPassword: string; newPassword: string }) =>
      changePassword(body.currentPassword, body.newPassword),
  });
}
