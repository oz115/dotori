import { useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import { useFocusEffect } from 'expo-router';
import { useCallback } from 'react';

import { notificationApi } from '@/features/notification/api/notificationApi';
import { notificationKeys } from '@/features/notification/model/notificationKeys';
import type { NotificationListFilter } from '@/features/notification/model/types';

/**
 * 알림함 목록, page 기반 무한 스크롤 (NOTI-01).
 *
 * useUnreadNotificationCount 와 같은 이유로, 이 화면에 다시 들어와도 캐시된 예전 목록을
 * 그대로 보여줄 수 있어서 들어올 때마다 명시적으로 다시 물어본다.
 */
export function useNotifications(filter: NotificationListFilter = {}) {
  const queryClient = useQueryClient();

  const query = useInfiniteQuery({
    queryKey: notificationKeys.list(filter),
    queryFn: ({ pageParam }) => notificationApi.getNotifications(filter, pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined,
  });

  useFocusEffect(
    useCallback(() => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.list(filter) });
      // filter 는 매 렌더 새 객체라 의존성에 넣으면 무한 루프가 된다 — queryKey 계산에
      // 이미 반영돼 있어서 여기선 queryClient 만으로 충분하다.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [queryClient]),
  );

  return query;
}
