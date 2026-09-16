import { useMutation, useQueryClient } from '@tanstack/react-query';

import { notificationApi } from '@/features/notification/api/notificationApi';
import { notificationKeys } from '@/features/notification/model/notificationKeys';

/** 알림 하나를 읽음 처리한다 (NOTI-03). 목록·안읽음 배지 캐시를 무효화한다. */
export function useMarkAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: number) => notificationApi.markAsRead(notificationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
