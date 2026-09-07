import { create } from 'zustand';

import type { AuthStatus, AuthUser } from '@/features/auth/model/types';

type AuthState = {
  status: AuthStatus;
  /** 메모리에만 유지. 앱을 껐다 켜면 refresh token 으로 새로 받는다. */
  accessToken: string | null;
  user: AuthUser | null;

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

  setSession: ({ accessToken, user }) => set({ accessToken, user, status: 'authenticated' }),
  setAccessToken: (accessToken) => set({ accessToken }),
  clearSession: () => set({ accessToken: null, user: null, status: 'guest' }),
}));
