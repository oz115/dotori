import { httpClient } from '@/shared/api';
import type {
  EmailConfirmResponse,
  EmailVerificationPurpose,
  LoginBody,
  LoginResponse,
  RefreshResponse,
  SignupBody,
  UserProfile,
  UserUpdateResponse,
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

  /**
   * 회원 정보 조회. 자동 로그인(토큰 복원)은 JWT 에 이름이 없어 세션에 이름이 비므로,
   * 마이페이지처럼 이름이 필요한 화면은 이 API 로 받아온다.
   */
  getProfile: (userId: number) =>
    httpClient.get<UserProfile>(`/api/v1/users/${userId}`).then((res) => res.data),

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

  /** 마이페이지 - 이름 변경 (부분 수정) */
  updateProfile: (userId: number, name: string) =>
    httpClient
      .patch<UserUpdateResponse>(`/api/v1/users/${userId}`, { name })
      .then((res) => res.data),

  /**
   * 마이페이지 - 비밀번호 변경. 현재 비밀번호가 틀리면 401 PASSWORD_MISMATCH.
   * 성공하면 서버가 기존 세션(현재 세션 포함)을 전부 폐기한다.
   */
  changePassword: (userId: number, currentPassword: string, newPassword: string) =>
    httpClient.patch(`/api/v1/users/${userId}/password`, { currentPassword, newPassword }),
};
