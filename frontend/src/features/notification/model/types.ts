export type DeviceTokenPlatform = 'ANDROID' | 'IOS';

/** POST/GET .../device-tokens 공통 응답 조각 */
export type DeviceTokenRegisterBody = {
  fcmToken: string;
  platform: DeviceTokenPlatform;
};

/** GET .../device-tokens 응답 (NOTI-10) */
export type DeviceTokenStatus = {
  registered: boolean;
};

export type NotificationType =
  | 'INVITATION'
  | 'PURCHASE'
  | 'CHARGE'
  | 'CASHOUT'
  | 'CHORE'
  | 'SAVINGS'
  | 'INVESTMENT'
  /** 아침 알림 — 오늘의 퀴즈·뉴스가 준비됐음을 알린다 (refId 없음) */
  | 'DAILY';

/** GET .../notifications 목록의 한 건 (NOTI-01) */
export type NotificationItem = {
  notificationId: number;
  type: NotificationType;
  title: string;
  body: string;
  refId: number | null;
  isRead: boolean;
  createdAt: string;
};

/** GET .../notifications 응답. unreadCount는 이번 조회 필터와 무관하게 항상 전체 미읽 개수. */
export type NotificationListResponse = {
  notifications: NotificationItem[];
  page: number;
  totalPages: number;
  unreadCount: number;
};

/** GET .../notifications 목록 조회 필터 (NOTI-01) */
export type NotificationListFilter = {
  type?: NotificationType;
  isRead?: boolean;
};

/** GET .../notifications/unread-count 응답 (NOTI-11) */
export type NotificationUnreadCount = {
  unreadCount: number;
};

/** PATCH .../notifications/{id}/read 응답 (NOTI-03) */
export type NotificationReadResult = {
  notificationId: number;
  isRead: boolean;
};
