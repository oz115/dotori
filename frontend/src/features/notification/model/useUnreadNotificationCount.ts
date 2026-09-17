import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useFocusEffect } from 'expo-router';
import { useCallback } from 'react';

import { notificationApi } from '@/features/notification/api/notificationApi';
import { notificationKeys } from '@/features/notification/model/notificationKeys';

/**
 * 벨 아이콘 배지용 안 읽은 알림 개수 (NOTI-11).
 *
 * 홈 탭은 전환해도 언마운트되지 않아서(useChoreRefresh 참고 — 같은 이유로 집안일 목록도
 * 늦게 반영됨), 부모 기기가 보낸 알림이 홈 화면에 이미 떠 있는 동안에는 절대 안 뜬다.
 * 화면에 들어올 때마다 명시적으로 다시 물어본다.
 */
export function useUnreadNotificationCount() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: () => notificationApi.getUnreadCount(),
    staleTime: 10_000,
  });

  useFocusEffect(
    useCallback(() => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    }, [queryClient]),
  );

  return query;
}
