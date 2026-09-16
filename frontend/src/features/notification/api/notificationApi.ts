import { httpClient } from '@/shared/api';
import type {
  DeviceTokenRegisterBody,
  DeviceTokenStatus,
} from '@/features/notification/model/types';

/** 디바이스 토큰 원시 HTTP 호출 (NOTI-08/09/10). */
export const notificationApi = {
  registerDeviceToken: (body: DeviceTokenRegisterBody) =>
    httpClient
      .post<{ deviceTokenId: number }>('/api/v1/device-tokens', body)
      .then((res) => res.data),

  unregisterDeviceToken: (fcmToken: string) =>
    httpClient
      .delete<void>('/api/v1/device-tokens', { params: { fcmToken } })
      .then(() => undefined),

  getDeviceTokenStatus: (fcmToken: string) =>
    httpClient
      .get<DeviceTokenStatus>('/api/v1/device-tokens', { params: { fcmToken } })
      .then((res) => res.data),
};
