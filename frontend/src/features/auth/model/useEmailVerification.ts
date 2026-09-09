import { useMutation } from '@tanstack/react-query';

import { authApi } from '@/features/auth/api/authApi';

/** 인증코드 발송 — POST /api/v1/auth/email-verification/send */
export function useSendEmailCode() {
  return useMutation({ mutationFn: (email: string) => authApi.emailVerification.send(email) });
}

/** 인증코드 확인 — verificationToken 을 돌려준다 */
export function useConfirmEmailCode() {
  return useMutation({
    mutationFn: ({ email, code }: { email: string; code: string }) =>
      authApi.emailVerification.confirm(email, code),
  });
}
