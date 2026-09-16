/** 알림/디바이스 토큰 관련 TanStack Query 키. */
export const notificationKeys = {
  all: ['notification'] as const,
  deviceTokenStatus: (fcmToken: string) =>
    [...notificationKeys.all, 'deviceTokenStatus', fcmToken] as const,
};
