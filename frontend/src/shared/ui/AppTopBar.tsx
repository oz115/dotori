import { Image, Pressable, StyleSheet, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type AppTopBarProps = {
  /** 프로필 아이콘 탭 — 마이페이지가 아직 없어 로그아웃/탈퇴 시트를 연다 */
  onProfilePress: () => void;
};

const ICON_SIZE = 28;

/**
 * 홈 화면 상단 바 (부모·자녀 공용). 워드마크 + 알림 + 프로필.
 * 알림함은 아직 없어 벨 아이콘은 자리만 잡아둔다(비활성).
 *
 * 여백은 여기서 직접 갖는다 — 쓰는 쪽이 감싸서 주지 않는다.
 */
export function AppTopBar({ onProfilePress }: AppTopBarProps) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.row,
        {
          paddingLeft: theme.layout.screenX,
          paddingRight: theme.layout.screenX,
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
        <Image
          source={require('@/assets/images/icons/icon-alert.png')}
          style={styles.icon}
          resizeMode="contain"
          accessibilityLabel="알림"
        />
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
});
