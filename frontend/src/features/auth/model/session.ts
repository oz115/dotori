import { authApi } from '@/features/auth/api/authApi';
import { decodeJwt } from '@/features/auth/lib/decodeJwt';
import { tokenStorage } from '@/features/auth/lib/tokenStorage';
import { useAuthStore } from '@/features/auth/model/authStore';
import type { LoginBody } from '@/features/auth/model/types';
import { queryClient } from '@/shared/api';
import { clearSessionScopedStorage } from '@/shared/lib/sessionScopedStorage';

/**
 * 세션 오케스트레이션 — HTTP 호출 결과를 토큰 저장소와 authStore 에 반영한다.
 * 화면에서는 이 함수들만 부르고 authApi 를 직접 부르지 않는다.
 */

/**
 * 세션 종료 시 남는 흔적을 전부 지운다.
 *
 * 서버 데이터 캐시(TanStack Query)는 앱 프로세스 단위라 계정을 바꿔도 그대로 남는다.
 * 특히 `['account','me']` 처럼 키에 사용자 정보가 없는 쿼리는 다음 사용자에게
 * 이전 사용자의 금융정보를 그대로 보여줄 수 있어, 로그아웃 시 통째로 비운다.
 */
async function clearLocalSession(): Promise<void> {
  await tokenStorage.clear();
  await clearSessionScopedStorage();
  useAuthStore.getState().clearSession();
  queryClient.clear();
}

/** 로그인: 토큰 저장 + 세션 설정 */
export async function signIn(body: LoginBody): Promise<void> {
  const res = await authApi.login(body);
  await tokenStorage.set(res.refreshToken);
  useAuthStore.getState().setSession({
    accessToken: res.accessToken,
    user: { userId: res.user.userId, role: res.user.role, name: res.user.name },
  });
}

/**
 * 회원 탈퇴 — 서버 soft delete 후 로컬 세션을 정리한다.
 * 비밀번호가 틀리면(PASSWORD_MISMATCH) throw 하고 세션은 유지된다.
 */
export async function withdrawAccount(password: string): Promise<void> {
  const userId = useAuthStore.getState().user?.userId;
  if (userId == null) throw new Error('세션이 없습니다');
  await authApi.withdraw(userId, password);
  await clearLocalSession();
}

/**
 * 비밀번호 변경 — 성공하면 서버가 기존 세션(현재 세션 포함)을 전부 폐기하므로
 * 로컬 세션도 함께 정리해 로그인 화면으로 돌려보낸다.
 * 현재 비밀번호가 틀리면(PASSWORD_MISMATCH) throw 하고 세션은 유지된다.
 */
export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  const userId = useAuthStore.getState().user?.userId;
  if (userId == null) throw new Error('세션이 없습니다');
  await authApi.changePassword(userId, currentPassword, newPassword);
  await clearLocalSession();
}

/** 로그아웃: 서버 무효화(실패 무시) + 로컬 정리 */
export async function signOut(): Promise<void> {
  const refreshToken = await tokenStorage.get();
  try {
    if (refreshToken) {
      await authApi.logout(refreshToken);
    }
  } catch {
    // 네트워크 문제 등으로 서버 로그아웃이 실패해도 로컬은 정리한다.
  }
  await clearLocalSession();
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
