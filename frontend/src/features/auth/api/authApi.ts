import { httpClient } from '@/shared/api';
import type {
  EmailConfirmResponse,
  EmailVerificationPurpose,
  LoginBody,
  LoginResponse,
  RefreshResponse,
  SignupBody,
} from '@/features/auth/model/types';

/**
 * 인증 관련 원시 HTTP 호출. 세션 상태 갱신·토큰 저장 같은 오케스트레이션은
 * `features/auth/model/session` 에서 담당한다.
 */
export const authApi = {
  login: (body: LoginBody) =>
    httpClient.post<LoginResponse>('/api/v1/auth/login', body).then((res) => res.data),

  signup: (body: SignupBody) =>
    httpClient.post('/api/v1/auth/signup', body).then((res) => res.data),

  logout: (refreshToken: string) => httpClient.post('/api/v1/auth/logout', { refreshToken }),

  refresh: (refreshToken: string) =>
    httpClient
      .post<RefreshResponse>('/api/v1/auth/refresh', { refreshToken })
      .then((res) => res.data),

  /** 아이디 중복 확인 — 사용 가능하면 204, 사용 중이면 409 DUPLICATE_LOGIN_ID */
  checkLoginId: (loginId: string) => httpClient.post('/api/v1/auth/login-id/check', { loginId }),

  emailVerification: {
    send: (email: string, purpose?: EmailVerificationPurpose) =>
      httpClient.post('/api/v1/auth/email-verification/send', { email, purpose }),
    confirm: (email: string, code: string) =>
      httpClient
        .post<EmailConfirmResponse>('/api/v1/auth/email-verification/confirm', { email, code })
        .then((res) => res.data),
  },

  findId: (body: { name: string; email: string; verificationToken: string }) =>
    httpClient.post<{ loginId: string }>('/api/v1/auth/find-id', body).then((res) => res.data),

  resetPassword: (body: {
    loginId: string;
    email: string;
    verificationToken: string;
    newPassword: string;
  }) => httpClient.post('/api/v1/auth/password-reset', body),

  /** 회원 탈퇴 (soft delete) — 본인 확인용 비밀번호 필요, 틀리면 PASSWORD_MISMATCH */
  withdraw: (userId: number, password: string) =>
    httpClient.delete(`/api/v1/users/${userId}`, { data: { password } }),
};
