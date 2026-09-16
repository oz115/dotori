import { router } from 'expo-router';

import { NotificationListScreen, resolveNotificationRoute } from '@/features/notification';

export default function ChildNotificationsScreen() {
  return (
    <NotificationListScreen
      onPressNotification={(item) => {
        const route = resolveNotificationRoute(
          { type: item.type, refId: item.refId == null ? undefined : String(item.refId) },
          'CHILD',
        );
        if (route) {
          router.push(route);
        }
      }}
    />
  );
}
