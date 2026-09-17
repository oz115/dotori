import { Ionicons } from '@expo/vector-icons';
import { useRef, type ComponentRef } from 'react';
import Swipeable from 'react-native-gesture-handler/ReanimatedSwipeable';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import Animated, {
  interpolateColor,
  useAnimatedStyle,
  type SharedValue,
} from 'react-native-reanimated';

import { formatNotificationTime } from '@/features/notification/lib/formatNotificationTime';
import { notificationTypeImage } from '@/features/notification/lib/notificationTypeMeta';
import type { NotificationItem } from '@/features/notification/model/types';
import { useTheme } from '@/shared/theme';

type NotificationRowProps = {
  item: NotificationItem;
  onPress: () => void;
  /** 삭제 요청. 실패하면 reject해서 스와이프를 원상복구할 수 있게 한다. */
  onDelete: () => Promise<unknown>;
};

/**
 * 알림함 목록의 한 행. 안 읽음이면 연초록 배경 + 제목 옆 초록 점으로 구분한다.
 * 왼쪽으로 스와이프하면 배경이 점점 빨개지고, 임계값을 넘겨서 놓으면 바로 삭제된다
 * (별도 삭제 버튼 없음 — 진해지는 빨간색 자체가 "놓으면 지워진다"는 신호).
 */
export function NotificationRow({ item, onPress, onDelete }: NotificationRowProps) {
  const theme = useTheme();
  const rowBg = item.isRead ? theme.color.background.surface : theme.color.brand.subtle;
  const swipeableRef = useRef<ComponentRef<typeof Swipeable>>(null);

  return (
    <Swipeable
      ref={swipeableRef}
      overshootRight={false}
      rightThreshold={80}
      renderRightActions={(progress) => (
        <DeleteBackdrop progress={progress} from={rowBg} to={theme.color.status.danger.solid} />
      )}
      onSwipeableOpen={(direction) => {
        if (direction !== 'right') return;
        // 삭제 실패 시(네트워크 오류 등) 스와이프를 원래 자리로 되돌린다.
        onDelete().catch(() => swipeableRef.current?.close());
      }}
    >
      <Pressable
        accessibilityRole="button"
        onPress={onPress}
        style={({ pressed }) => [
          styles.row,
          {
            backgroundColor: rowBg,
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
          <Text
            numberOfLines={2}
            style={[theme.typography.bodySm, { color: theme.color.text.sub }]}
          >
            {item.body}
          </Text>
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            {formatNotificationTime(item.createdAt)}
          </Text>
        </View>

        <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
      </Pressable>
    </Swipeable>
  );
}

type DeleteBackdropProps = {
  progress: SharedValue<number>;
  from: string;
  to: string;
};

/** 스와이프 진행도(0→1)에 맞춰 배경색과 휴지통 아이콘을 점점 진하게/크게 보여준다. */
function DeleteBackdrop({ progress, from, to }: DeleteBackdropProps) {
  const theme = useTheme();
  const containerStyle = useAnimatedStyle(() => ({
    backgroundColor: interpolateColor(progress.value, [0, 1], [from, to]),
    borderRadius: theme.radius.lg,
  }));
  const iconStyle = useAnimatedStyle(() => ({
    opacity: Math.min(progress.value, 1),
    transform: [{ scale: 0.6 + Math.min(progress.value, 1) * 0.4 }],
  }));

  return (
    <Animated.View style={[styles.deleteBackdrop, containerStyle]}>
      <Animated.View style={iconStyle}>
        <Ionicons name="trash" size={22} color="#FFFFFF" />
      </Animated.View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center' },
  icon: { width: 44, height: 44 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  title: { flexShrink: 1 },
  dot: { width: 6, height: 6, borderRadius: 3 },
  deleteBackdrop: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'flex-end',
    paddingRight: 24,
  },
});
