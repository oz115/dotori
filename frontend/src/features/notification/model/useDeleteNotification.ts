import { useMutation, useQueryClient } from '@tanstack/react-query';

import { notificationApi } from '@/features/notification/api/notificationApi';
import { notificationKeys } from '@/features/notification/model/notificationKeys';

/** 알림 하나를 삭제한다. 목록·안읽음 배지 캐시를 무효화한다. */
export function useDeleteNotification() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: number) => notificationApi.deleteNotification(notificationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
