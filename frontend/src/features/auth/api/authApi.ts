import { httpClient } from '@/shared/api';
import type {
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
};
