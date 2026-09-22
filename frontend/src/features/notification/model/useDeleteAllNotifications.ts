import { useMutation, useQueryClient } from '@tanstack/react-query';

import { notificationApi } from '@/features/notification/api/notificationApi';
import { notificationKeys } from '@/features/notification/model/notificationKeys';

/**
 * 알림 전부를 삭제한다 (알림함 "모두 삭제"). 목록·안읽음 배지 캐시를 무효화한다.
 *
 * <p>무효화를 await 한다 — 목록이 아직 옛 데이터일 때 화면이 먼저 갱신되면 지운 알림이
 * 한 번 깜빡였다가 사라진다 (푸시 설정 스위치에서 겪은 것과 같은 문제).
 */
export function useDeleteAllNotifications() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationApi.deleteAllNotifications(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}
