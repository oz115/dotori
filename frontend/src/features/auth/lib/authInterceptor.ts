import type { AxiosError, InternalAxiosRequestConfig } from 'axios';

import { httpClient } from '@/shared/api';
import { refreshAccessToken } from '@/features/auth/model/session';
import { useAuthStore } from '@/features/auth/model/authStore';
import { tokenStorage } from '@/features/auth/lib/tokenStorage';

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

let installed = false;
/** 동시에 여러 요청이 401 을 받아도 refresh 는 한 번만 돈다. */
let pendingRefresh: Promise<string> | null = null;

function isAuthEndpoint(url?: string): boolean {
  return !!url && /\/api\/v1\/auth\/(login|refresh|logout|signup)/.test(url);
}

/**
 * httpClient 에 인증 인터셉터를 붙인다. 앱 부팅 시 한 번 호출.
 * - 요청: 메모리의 access token 을 Authorization 헤더로
 * - 응답 401: access token 만료로 보고 refresh 후 원요청 1회 재시도.
 *   refresh 도 실패하면 세션 정리(guest).
 */
export function setupAuthInterceptor(): void {
  if (installed) {
    return;
  }
  installed = true;

  httpClient.interceptors.request.use((config) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  httpClient.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const original = error.config as RetriableConfig | undefined;

      if (
        error.response?.status !== 401 ||
        !original ||
        original._retried ||
        isAuthEndpoint(original.url)
      ) {
        return Promise.reject(error);
      }

      original._retried = true;
      try {
        pendingRefresh ??= refreshAccessToken().finally(() => {
          pendingRefresh = null;
        });
        const newToken = await pendingRefresh;
        original.headers.Authorization = `Bearer ${newToken}`;
        return httpClient(original);
      } catch {
        await tokenStorage.clear();
        useAuthStore.getState().clearSession();
        return Promise.reject(error);
      }
    },
  );
}
