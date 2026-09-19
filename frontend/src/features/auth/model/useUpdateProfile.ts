// Created: 2026-09-19 13:19:00
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { authApi } from '@/features/auth/api/authApi';
import { useAuthStore } from '@/features/auth/model/authStore';

/** 마이페이지 - 이름 변경 mutation. 성공하면 프로필 쿼리(useProfile)를 갱신한다. */
export function useUpdateProfile() {
  const queryClient = useQueryClient();
  const userId = useAuthStore((s) => s.user?.userId);

  return useMutation({
    mutationFn: (name: string) => {
      if (userId == null) throw new Error('세션이 없습니다');
      return authApi.updateProfile(userId, name);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['auth', 'profile'] });
    },
  });
}
