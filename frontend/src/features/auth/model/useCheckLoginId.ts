import { useMutation } from '@tanstack/react-query';

import { authApi } from '@/features/auth/api/authApi';

/**
 * 아이디 중복 확인 — 사용 가능하면 성공(204), 사용 중이면 실패(409 DUPLICATE_LOGIN_ID).
 */
export function useCheckLoginId() {
  return useMutation({ mutationFn: (loginId: string) => authApi.checkLoginId(loginId) });
}
