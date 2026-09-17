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
    // Swipeable 내부 컨테이너는 스와이프 콘텐츠를 가리려고 overflow:hidden을 강제로 갖는다.
    // 카드 그림자(Android는 elevation)를 그 안쪽에 주면 클리핑에 걸려 모서리가 잘려 보여서,
    // 안 잘리는 이 바깥 View로 그림자를 옮겼다.
    <View style={[{ borderRadius: theme.radius.lg }, item.isRead ? theme.shadow.card : null]}>
      <Swipeable
        ref={swipeableRef}
        overshootFriction={8}
        rightThreshold={80}
        renderRightActions={(progress, translation) => (
          <DeleteBackdrop
            progress={progress}
            translation={translation}
            from={rowBg}
            to={theme.color.status.danger.solid}
          />
        )}
        onSwipeableOpen={() => {
          // renderLeftActions가 없어서 onSwipeableOpen은 항상 이 오른쪽 삭제 패널이 열릴
          // 때만 불린다. (참고: direction 값은 "열린 패널 쪽"이 아니라 "스와이프한 방향"이라
          // 오른쪽 패널을 열기 위해 왼쪽으로 스와이프하면 'left'로 온다 — 그래서 direction으로
          // 분기하지 않는다.)
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
          ]}
        >
          <Image
            source={notificationTypeImage(item.type)}
            style={styles.icon}
            resizeMode="contain"
          />

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
    </View>
  );
}

const ACTION_WIDTH = 96;

type DeleteBackdropProps = {
  progress: SharedValue<number>;
  translation: SharedValue<number>;
  from: string;
  to: string;
};

/**
 * 스와이프 진행도(0→1)에 맞춰 배경색과 휴지통 아이콘을 점점 진하게/크게 보여준다.
 *
 * gesture-handler는 제스처 "시작" 시점에 이 컴포넌트가 차지하는 고정폭(=96)을 한 번
 * 측정해서 스냅 목표/스프링 거리로 쓴다 — 그 측정용 박스(deleteBackdropSlot) 폭을
 * 드래그 중에 같이 키우면 목표 거리가 계속 바뀌어서 스프링이 못 끝나는 문제가 재발한다
 * (실측: 카드 전체 너비로 측정되면 삭제 콜백 자체가 영영 안 불림). 그래서 측정용 박스는
 * 96 고정으로 두고, 실제 눈에 보이는 빨간 영역(deleteBackdropFill)만 절대 위치로 덧씌워
 * translation(드래그 거리)만큼 계속 넓어지게 한다 — 손가락을 계속 따라오는 것처럼 보인다.
 */
function DeleteBackdrop({ progress, translation, from, to }: DeleteBackdropProps) {
  const theme = useTheme();
  const fillStyle = useAnimatedStyle(() => ({
    width: Math.max(Math.abs(translation.value), ACTION_WIDTH),
    backgroundColor: interpolateColor(progress.value, [0, 1], [from, to]),
    borderRadius: theme.radius.lg,
  }));
  const iconStyle = useAnimatedStyle(() => ({
    opacity: Math.min(progress.value, 1),
    transform: [{ scale: 0.6 + Math.min(progress.value, 1) * 0.4 }],
  }));

  return (
    <View style={styles.deleteBackdropSlot}>
      <Animated.View style={[styles.deleteBackdropFill, fillStyle]}>
        <Animated.View style={iconStyle}>
          <Ionicons name="trash" size={22} color="#FFFFFF" />
        </Animated.View>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center' },
  icon: { width: 44, height: 44 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  title: { flexShrink: 1 },
  dot: { width: 6, height: 6, borderRadius: 3 },
  // gesture-handler가 스냅 목표 거리로 측정하는 고정폭 박스. 실제로는 안 보이고
  // (deleteBackdropFill이 그 위를 덮는다) 자리만 차지한다.
  deleteBackdropSlot: { width: ACTION_WIDTH, height: '100%' },
  // 드래그 거리만큼 늘어나는 실제 빨간 영역. 오른쪽에 고정해 카드 오른쪽 끝을 기준으로
  // 왼쪽으로 자라난다.
  deleteBackdropFill: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
