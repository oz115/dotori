import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type AppTopBarProps = {
  /** 프로필 아이콘 탭 — 마이페이지가 아직 없어 로그아웃/탈퇴 시트를 연다 */
  onProfilePress: () => void;
  /**
   * 알림 벨 탭. (child)/(parent) 둘 다 `/notifications`가 있어서, 그룹 접두사 없는
   * 경로로 push하면 expo-router가 조용히 무시해버린다 — 그래서 이 컴포넌트가 직접
   * push하지 않고, 어느 그룹인지 아는 쪽(ChildHome/ParentHome)이 콜백으로 넘긴다.
   */
  onNotificationsPress: () => void;
  /** 안 읽은 알림 수. 0 이거나 없으면 배지를 숨긴다. */
  unreadCount?: number;
};

const ICON_SIZE = 28;

/**
 * 홈 화면 상단 바 (부모·자녀 공용). 워드마크 + 알림 + 프로필.
 *
 * 여백은 여기서 직접 갖는다 — 쓰는 쪽이 감싸서 주지 않는다.
 */
export function AppTopBar({ onProfilePress, onNotificationsPress, unreadCount }: AppTopBarProps) {
  const theme = useTheme();
  const hasUnread = (unreadCount ?? 0) > 0;

  return (
    <View
      style={[
        styles.row,
        {
          paddingHorizontal: theme.space['2xl'],
          paddingTop: theme.space.sm,
          marginBottom: theme.space.lg,
        },
      ]}
    >
      <Image
        source={require('@/assets/images/icons/icon-logo.png')}
        style={styles.wordmark}
        resizeMode="contain"
        accessibilityLabel="도토리"
      />
      <View style={styles.actions}>
        <Pressable
          onPress={onNotificationsPress}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="알림"
          style={styles.bellWrap}
        >
          <Image
            source={require('@/assets/images/icons/icon-alert.png')}
            style={styles.icon}
            resizeMode="contain"
          />
          {hasUnread ? (
            <View
              style={[
                styles.badge,
                {
                  backgroundColor: theme.color.status.danger.solid,
                  borderRadius: theme.radius.full,
                  borderColor: theme.color.background.canvas,
                },
              ]}
            >
              <Text style={[theme.typography.caption, styles.badgeText]} numberOfLines={1}>
                {unreadCount && unreadCount > 99 ? '99+' : unreadCount}
              </Text>
            </View>
          ) : null}
        </Pressable>
        <Pressable
          onPress={onProfilePress}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="내 계정"
        >
          <Image
            source={require('@/assets/images/icons/icon-mypage.png')}
            style={styles.icon}
            resizeMode="contain"
          />
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  wordmark: { width: 93, height: 34 }, // icon-logo.png 원본 비율(308×113 ≈ 2.73:1)에 맞춤 — 다르면 contain이 레터박스를 만든다
  actions: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  icon: { width: ICON_SIZE, height: ICON_SIZE },
  bellWrap: { position: 'relative' },
  badge: {
    position: 'absolute',
    top: -4,
    right: -6,
    minWidth: 16,
    height: 16,
    paddingHorizontal: 3,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { color: '#FFFFFF', fontSize: 10, lineHeight: 12 },
});
