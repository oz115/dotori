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

  /**
   * 누른 직후의 위치. 서버 응답을 기다리는 동안 스위치를 여기에 붙잡아 둔다.
   *
   * 이게 없으면 스위치가 좌우로 두 번 움직였다 (QA). 네이티브 스위치는 누르는 즉시 반대쪽으로
   * 움직이는데, 곧이어 다시 그려질 때 value 가 아직 서버의 옛 값이라 제자리로 튕겨 갔다가,
   * 응답이 와서야 다시 넘어가기 때문이다.
   */
  const [pendingValue, setPendingValue] = useState<boolean | null>(null);

  const invalidateStatus = async () => {
    if (fcmToken != null) {
      // 다시 받아올 때까지 기다린다. 기다리지 않고 pendingValue 를 비우면 그 사이 옛 값이
      // 잠깐 보여서 다시 튄다.
      await queryClient.invalidateQueries({
        queryKey: notificationKeys.deviceTokenStatus(fcmToken),
      });
    }
  };

  const enable = useMutation({
    // 마이페이지에서 직접 켜는 거라, 예전에 거부했어도 권한을 다시 물어본다.
    mutationFn: () => ensurePushRegistered(true),
    onSuccess: invalidateStatus,
    // 실패하든 성공하든 붙잡아 둔 값을 놓는다. 권한을 거부하면 서버에는 등록되지 않으므로
    // 스위치가 꺼짐으로 돌아가는데, 그건 실제 상태라 맞는 움직임이다.
    onSettled: () => setPendingValue(null),
  });

  const disable = useMutation({
    mutationFn: async () => {
      if (fcmToken == null) return;
      await notificationApi.unregisterDeviceToken(fcmToken);
    },
    onSuccess: invalidateStatus,
    onSettled: () => setPendingValue(null),
  });

  const registered = status.data?.registered ?? false;
  const enabled = pendingValue ?? registered;

  return {
    enabled,
    isLoading: fcmToken == null || status.isPending,
    isToggling: enable.isPending || disable.isPending,
    toggle: () => {
      const next = !enabled;
      setPendingValue(next);
      (next ? enable : disable).mutate();
    },
  };
}
