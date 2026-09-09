import { useMutation } from '@tanstack/react-query';

import { authApi } from '@/features/auth/api/authApi';
import type { EmailVerificationPurpose } from '@/features/auth/model/types';

type SendEmailCodeArgs = { email: string; purpose?: EmailVerificationPurpose };

/**
 * 인증코드 발송 — POST /api/v1/auth/email-verification/send
 * purpose 로 용도를 알려주면 서버가 그에 맞게 처리한다.
 * - SIGNUP: 이미 가입된 이메일이면 발송하지 않고 409 DUPLICATE_EMAIL
 * - FIND_ID / PASSWORD_RESET / 생략: 계정 존재 여부와 무관하게 발송
 */
export function useSendEmailCode() {
  return useMutation({
    mutationFn: ({ email, purpose }: SendEmailCodeArgs) =>
      authApi.emailVerification.send(email, purpose),
  });
}

/** 인증코드 확인 — verificationToken 을 돌려준다 */
export function useConfirmEmailCode() {
  return useMutation({
    mutationFn: ({ email, code }: { email: string; code: string }) =>
      authApi.emailVerification.confirm(email, code),
  });
}
