import { useMutation } from '@tanstack/react-query';

import { authApi } from '@/features/auth/api/authApi';
import type { SignupBody } from '@/features/auth/model/types';

/**
 * 회원가입 mutation. `POST /api/v1/auth/signup`.
 * 성공해도 세션 토큰은 없으므로, 화면에서 로그인 화면으로 보낸다.
 */
export function useSignup() {
  return useMutation({ mutationFn: (body: SignupBody) => authApi.signup(body) });
}
