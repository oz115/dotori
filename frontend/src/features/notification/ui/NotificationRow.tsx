import { Ionicons } from '@expo/vector-icons';
import { useRef, type ComponentRef } from 'react';
import Swipeable from 'react-native-gesture-handler/ReanimatedSwipeable';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

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

/** 스와이프해서 드러나는 삭제 버튼의 폭 */
const ACTION_WIDTH = 80;

/**
 * 알림함 목록의 한 행.
 *
 * 왼쪽으로 밀면 빨간 휴지통 버튼이 드러나고, 그 버튼을 눌러야 지워진다. 예전에는 임계값을
 * 넘겨 놓기만 해도 바로 삭제돼서 알림이 갑자기 사라졌다 (QA) - 되돌릴 방법이 없는 동작은
 * 한 번 더 누르게 한다.
 *
 * <p>카드 테두리는 읽음 여부와 상관없이 같다. 전에는 읽은 행에만 그림자를 줘서 "모두 읽음"
 * 을 누르는 순간 목록 전체의 테두리가 바뀌고, 섞여 있을 때는 행마다 있다 없다 했다 (QA).
 * 읽음 여부는 배경 톤과 제목 옆 초록 점으로만 구분한다.
 */
export function NotificationRow({ item, onPress, onDelete }: NotificationRowProps) {
  const theme = useTheme();
  const rowBg = item.isRead ? theme.color.background.surface : theme.color.brand.subtle;
  const swipeableRef = useRef<ComponentRef<typeof Swipeable>>(null);

  const handleDelete = () => {
    // 실패하면(네트워크 오류 등) 열려 있던 스와이프를 원래 자리로 되돌린다.
    onDelete().catch(() => swipeableRef.current?.close());
  };

  return (
    // Swipeable 내부 컨테이너는 스와이프 콘텐츠를 가리려고 overflow:hidden을 강제로 갖는다.
    // 카드 그림자(Android는 elevation)를 그 안쪽에 주면 클리핑에 걸려 모서리가 잘려 보여서,
    // 안 잘리는 이 바깥 View로 테두리·그림자를 옮겼다.
    <View
      style={[
        styles.card,
        {
          borderColor: theme.color.border.card,
          borderRadius: theme.radius.lg,
          backgroundColor: rowBg,
        },
        theme.shadow.contentCard,
      ]}
    >
      <Swipeable
        ref={swipeableRef}
        overshootRight={false}
        rightThreshold={ACTION_WIDTH / 2}
        renderRightActions={() => (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${item.title} 알림 삭제`}
            onPress={handleDelete}
            style={({ pressed }) => [
              styles.deleteButton,
              {
                backgroundColor: theme.color.status.danger.solid,
                opacity: pressed ? 0.85 : 1,
              },
            ]}
          >
            <Ionicons name="trash" size={22} color="#FFFFFF" />
          </Pressable>
        )}
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
        </Pressable>
      </Swipeable>
    </View>
  );
}

const styles = StyleSheet.create({
  // 안쪽 Swipeable 이 클리핑하도록 바깥에서 모서리를 자른다 (그래야 빨간 버튼도 둥글게 잘린다)
  card: { borderWidth: 1, overflow: 'hidden' },
  row: { flexDirection: 'row', alignItems: 'center' },
  icon: { width: 44, height: 44 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  title: { flexShrink: 1 },
  dot: { width: 6, height: 6, borderRadius: 3 },
  deleteButton: { width: ACTION_WIDTH, alignItems: 'center', justifyContent: 'center' },
});
