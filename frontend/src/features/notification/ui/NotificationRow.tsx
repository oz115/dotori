import { Ionicons } from '@expo/vector-icons';
import { Alert, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { formatNotificationTime } from '@/features/notification/lib/formatNotificationTime';
import { notificationTypeImage } from '@/features/notification/lib/notificationTypeMeta';
import type { NotificationItem } from '@/features/notification/model/types';
import { useTheme } from '@/shared/theme';

type NotificationRowProps = {
  item: NotificationItem;
  onPress: () => void;
  /** 삭제 요청. 확인을 받은 뒤에만 호출한다. */
  onDelete: () => Promise<unknown>;
};

/**
 * 알림함 목록의 한 행.
 *
 * <p>오른쪽 휴지통은 항상 보인다. 전에는 왼쪽으로 밀어야 드러났는데, 밀 수 있다는 것을
 * 알 방법이 없어서 지우는 기능이 있는 줄도 몰랐다 (QA). 지우는 일은 되돌릴 수 없으니
 * 누르면 한 번 더 확인받는다 — 아이가 쓰는 화면이라 오탭이 곧 알림 유실이 된다.
 *
 * <p>카드 테두리는 읽음 여부와 상관없이 같다. 전에는 읽은 행에만 그림자를 줘서 "모두 읽음"
 * 을 누르는 순간 목록 전체의 테두리가 바뀌고, 섞여 있을 때는 행마다 있다 없다 했다 (QA).
 * 읽음 여부는 배경 톤과 제목 옆 초록 점으로만 구분한다.
 */
export function NotificationRow({ item, onPress, onDelete }: NotificationRowProps) {
  const theme = useTheme();
  const rowBg = item.isRead ? theme.color.background.surface : theme.color.brand.subtle;

  const confirmDelete = () => {
    Alert.alert('이 알림을 지울까요?', '지운 알림은 다시 볼 수 없어요.', [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        // 실패(네트워크 오류 등)는 목록이 그대로 남는 것으로 드러난다 — 여기서 더 알릴 것이 없다.
        onPress: () => void onDelete().catch(() => undefined),
      },
    ]);
  };

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        theme.shadow.contentCard,
        {
          borderColor: theme.color.border.card,
          borderRadius: theme.radius.lg,
          backgroundColor: rowBg,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.9 : 1,
        },
      ]}
    >
      <Image source={notificationTypeImage(item.type)} style={styles.icon} resizeMode="contain" />

      <View style={styles.body}>
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

      {/* 본문 옆 보조 동작이라 작게 둔다. 대신 hitSlop 으로 실제 누를 수 있는 영역을 넓힌다 */}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${item.title} 알림 삭제`}
        onPress={confirmDelete}
        hitSlop={12}
        style={({ pressed }) => [styles.deleteButton, { opacity: pressed ? 0.5 : 1 }]}
      >
        <Ionicons name="trash-outline" size={18} color={theme.color.status.danger.solid} />
      </Pressable>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', borderWidth: 1 },
  icon: { width: 44, height: 44 },
  body: { flex: 1, gap: 4 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  title: { flexShrink: 1 },
  dot: { width: 6, height: 6, borderRadius: 3 },
  deleteButton: { alignItems: 'center', justifyContent: 'center' },
});
