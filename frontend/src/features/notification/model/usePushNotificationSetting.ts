import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';

import { notificationApi } from '@/features/notification/api/notificationApi';
import { getFcmToken } from '@/features/notification/lib/pushToken';
import { ensurePushRegistered } from '@/features/notification/model/ensurePushRegistered';
import { notificationKeys } from '@/features/notification/model/notificationKeys';

/**
 * 마이페이지 푸시 알림 토글 (NOTI-10 기반). 이 기기의 FCM 토큰을 먼저 구하고, 그 토큰이
 * 서버에 등록돼 있는지로 켜짐/꺼짐을 판단한다 — 로컬에 따로 상태를 기억해두지 않아
 * 재설치·재로그인해도 항상 서버 기준 값을 보여준다.
 *
 * iOS는 FCM 연동이 없어 토큰을 못 구하므로 항상 꺼짐(비활성)으로 취급한다.
 */
export function usePushNotificationSetting() {
  const queryClient = useQueryClient();
  const [fcmToken, setFcmToken] = useState<string | null>(null);

  useEffect(() => {
    getFcmToken().then(setFcmToken);
  }, []);

  const status = useQuery({
    queryKey: notificationKeys.deviceTokenStatus(fcmToken ?? ''),
    queryFn: () => notificationApi.getDeviceTokenStatus(fcmToken as string),
    enabled: fcmToken != null,
  });

  const invalidateStatus = () => {
    if (fcmToken != null) {
      queryClient.invalidateQueries({ queryKey: notificationKeys.deviceTokenStatus(fcmToken) });
    }
  };

  const enable = useMutation({
    // 마이페이지에서 직접 켜는 거라, 예전에 거부했어도 권한을 다시 물어본다.
    mutationFn: () => ensurePushRegistered(true),
    onSuccess: invalidateStatus,
  });

  const disable = useMutation({
    mutationFn: async () => {
      if (fcmToken == null) return;
      await notificationApi.unregisterDeviceToken(fcmToken);
    },
    onSuccess: invalidateStatus,
  });

  return {
    enabled: status.data?.registered ?? false,
    isLoading: fcmToken == null || status.isPending,
    isToggling: enable.isPending || disable.isPending,
    toggle: () => (status.data?.registered ? disable.mutate() : enable.mutate()),
  };
}
