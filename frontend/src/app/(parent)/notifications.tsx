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
          router.push(route);
        }
      }}
    />
  );
}
