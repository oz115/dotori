import { useQuery } from '@tanstack/react-query';

import { authApi } from '@/features/auth/api/authApi';
import { useAuthStore } from '@/features/auth/model/authStore';

/**
 * 로그인한 본인의 회원 정보.
 *
 * 세션에도 이름이 있지만 로그인 직후에만 채워진다 — 앱을 껐다 켜면 토큰(JWT)으로 세션을
 * 복원하는데 거기엔 이름이 없다. 그래서 이름을 보여주는 화면은 서버 값을 쓴다.
 */
export function useProfile() {
  const userId = useAuthStore((s) => s.user?.userId);

  return useQuery({
    queryKey: ['auth', 'profile', userId ?? -1],
    queryFn: () => authApi.getProfile(userId as number),
    enabled: userId != null,
    staleTime: 5 * 60 * 1000,
  });
}
