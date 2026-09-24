import { create } from 'zustand';

import type { AuthStatus, AuthUser } from '@/features/auth/model/types';

type AuthState = {
  status: AuthStatus;
  /** 메모리에만 유지. 앱을 껐다 켜면 refresh token 으로 새로 받는다. */
  accessToken: string | null;
  user: AuthUser | null;
  /**
   * 이번 세션이 시작된 시각(ISO). 서버가 "마지막 접속"을 내려주지 않아서
   * 마이페이지 표시는 이 값을 쓴다 — 서버에 필드가 생기면 그쪽으로 옮긴다.
   */
  signedInAt: string | null;

  setSession: (session: { accessToken: string; user: AuthUser }) => void;
  setAccessToken: (accessToken: string) => void;
  clearSession: () => void;
};

/**
 * 클라이언트 인증 상태. 서버 데이터(프로필 등)는 여기 두지 않고 TanStack Query 로.
 */
export const useAuthStore = create<AuthState>((set) => ({
  status: 'loading',
  accessToken: null,
  user: null,
  signedInAt: null,

  setSession: ({ accessToken, user }) =>
    set({ accessToken, user, status: 'authenticated', signedInAt: new Date().toISOString() }),
  setAccessToken: (accessToken) => set({ accessToken }),
  clearSession: () => set({ accessToken: null, user: null, status: 'guest', signedInAt: null }),
}));
