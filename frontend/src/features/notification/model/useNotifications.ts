import { useInfiniteQuery } from '@tanstack/react-query';

import { notificationApi } from '@/features/notification/api/notificationApi';
import { notificationKeys } from '@/features/notification/model/notificationKeys';
import type { NotificationListFilter } from '@/features/notification/model/types';

/** 알림함 목록, page 기반 무한 스크롤 (NOTI-01). */
export function useNotifications(filter: NotificationListFilter = {}) {
  return useInfiniteQuery({
    queryKey: notificationKeys.list(filter),
    queryFn: ({ pageParam }) => notificationApi.getNotifications(filter, pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined,
  });
}
