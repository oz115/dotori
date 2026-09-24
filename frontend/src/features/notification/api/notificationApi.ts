import { httpClient } from '@/shared/api';
import type {
  DeviceTokenRegisterBody,
  DeviceTokenStatus,
  NotificationListFilter,
  NotificationListResponse,
  NotificationReadResult,
  NotificationUnreadCount,
} from '@/features/notification/model/types';

/** 디바이스 토큰 원시 HTTP 호출 (NOTI-08/09/10) + 알림함 원시 HTTP 호출 (NOTI-01/03/04/11). */
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

  getNotifications: (filter: NotificationListFilter, page: number) =>
    httpClient
      .get<NotificationListResponse>('/api/v1/notifications', {
        params: { ...filter, page, size: 20 },
      })
      .then((res) => res.data),

  markAsRead: (notificationId: number) =>
    httpClient
      .patch<NotificationReadResult>(`/api/v1/notifications/${notificationId}/read`)
      .then((res) => res.data),

  markAllAsRead: () =>
    httpClient
      .patch<{ updatedCount: number }>('/api/v1/notifications/read-all')
      .then((res) => res.data),

  getUnreadCount: () =>
    httpClient
      .get<NotificationUnreadCount>('/api/v1/notifications/unread-count')
      .then((res) => res.data),

  deleteNotification: (notificationId: number) =>
    httpClient.delete<void>(`/api/v1/notifications/${notificationId}`).then(() => undefined),

  /** 본인 알림 전부 삭제. 지울 것이 없어도 0 건으로 성공한다. */
  deleteAllNotifications: () =>
    httpClient.delete<{ deletedCount: number }>('/api/v1/notifications').then((res) => res.data),
};
