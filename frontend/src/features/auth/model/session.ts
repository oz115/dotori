import { authApi } from '@/features/auth/api/authApi';
import { decodeJwt } from '@/features/auth/lib/decodeJwt';
import { tokenStorage } from '@/features/auth/lib/tokenStorage';
import { useAuthStore } from '@/features/auth/model/authStore';
import type { LoginBody } from '@/features/auth/model/types';

/**
 * 세션 오케스트레이션 — HTTP 호출 결과를 토큰 저장소와 authStore 에 반영한다.
 * 화면에서는 이 함수들만 부르고 authApi 를 직접 부르지 않는다.
 */

/** 로그인: 토큰 저장 + 세션 설정 */
export async function signIn(body: LoginBody): Promise<void> {
  const res = await authApi.login(body);
  await tokenStorage.set(res.refreshToken);
  useAuthStore.getState().setSession({
    accessToken: res.accessToken,
    user: { userId: res.user.userId, role: res.user.role, name: res.user.name },
  });
}

/** 로그아웃: 서버 무효화(실패 무시) + 로컬 정리 */
export async function signOut(): Promise<void> {
  try {
    await authApi.logout();
  } catch {
    // 네트워크 문제 등으로 서버 로그아웃이 실패해도 로컬은 정리한다.
  }
  await tokenStorage.clear();
  useAuthStore.getState().clearSession();
}

/**
 * refresh token 으로 access token 재발급. 인터셉터(401 처리)와 부팅 복원이 공유한다.
 * refresh token 이 없거나 만료면 throw.
 */
export async function refreshAccessToken(): Promise<string> {
  const refreshToken = await tokenStorage.get();
  if (!refreshToken) {
    throw new Error('no refresh token');
  }
  const res = await authApi.refresh(refreshToken);
  await tokenStorage.set(res.refreshToken);
  useAuthStore.getState().setAccessToken(res.accessToken);
  return res.accessToken;
}

/**
 * 앱 부팅 시 저장된 refresh token 으로 세션 복원.
 * 성공하면 authenticated, 실패/없음이면 guest 로 확정한다.
 */
export async function restoreSession(): Promise<void> {
  const store = useAuthStore.getState();
  try {
    const accessToken = await refreshAccessToken();
    const claims = decodeJwt(accessToken);
    store.setSession({
      accessToken,
      user: { userId: Number(claims.sub), role: claims.role },
    });
  } catch {
    await tokenStorage.clear();
    store.clearSession();
  }
}
