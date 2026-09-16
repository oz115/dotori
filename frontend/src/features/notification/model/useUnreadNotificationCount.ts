import { useQuery } from '@tanstack/react-query';

import { notificationApi } from '@/features/notification/api/notificationApi';
import { notificationKeys } from '@/features/notification/model/notificationKeys';

/** 벨 아이콘 배지용 안 읽은 알림 개수 (NOTI-11). */
export function useUnreadNotificationCount() {
  return useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: () => notificationApi.getUnreadCount(),
    staleTime: 10_000,
  });
}
