import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { formatNotificationTime } from '@/features/notification/lib/formatNotificationTime';
import { notificationTypeImage } from '@/features/notification/lib/notificationTypeMeta';
import type { NotificationItem } from '@/features/notification/model/types';
import { useTheme } from '@/shared/theme';

type NotificationRowProps = {
  item: NotificationItem;
  onPress: () => void;
};

/** 알림함 목록의 한 행. 안 읽음이면 연초록 배경 + 제목 옆 초록 점으로 구분한다. */
export function NotificationRow({ item, onPress }: NotificationRowProps) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        {
          backgroundColor: item.isRead ? theme.color.background.surface : theme.color.brand.subtle,
          borderRadius: theme.radius.lg,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.9 : 1,
        },
        item.isRead ? theme.shadow.card : null,
      ]}
    >
      <Image source={notificationTypeImage(item.type)} style={styles.icon} resizeMode="contain" />

      <View style={{ flex: 1, gap: 4 }}>
        <View style={styles.titleRow}>
          <Text
            numberOfLines={1}
            style={[theme.typography.label, styles.title, { color: theme.color.text.strong }]}
          >
            {item.title}
          </Text>
          {item.isRead ? null : (
            <View style={[styles.dot, { backgroundColor: theme.color.brand.solid }]} />
          )}
        </View>
        <Text numberOfLines={2} style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
          {item.body}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          {formatNotificationTime(item.createdAt)}
        </Text>
      </View>

      <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center' },
  icon: { width: 44, height: 44 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  title: { flexShrink: 1 },
  dot: { width: 6, height: 6, borderRadius: 3 },
});
