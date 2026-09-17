import { router } from 'expo-router';

import { NotificationListScreen, resolveNotificationRoute } from '@/features/notification';

export default function ParentNotificationsScreen() {
  return (
    <NotificationListScreen
      onPressNotification={(item) => {
        const route = resolveNotificationRoute(
          { type: item.type, refId: item.refId == null ? undefined : String(item.refId) },
          'PARENT',
        );
        if (route) {
          // push는 (tabs)가 이미 스택에 있어도 새 인스턴스를 하나 더 쌓아서 탭 인스턴스가
          // 두 개로 쪼개진다 — navigate로 기존 (tabs) 인스턴스를 재사용한다.
          router.navigate(route);
        }
      }}
    />
  );
}
