import { Ionicons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useTheme } from '@/shared/theme';

type HeaderProps = {
  title: string;
  /** 넘기면 좌측에 뒤로가기 화살표가 나온다 */
  onBack?: () => void;
  /** 우측 텍스트 액션 (Figma: "취소") */
  action?: { label: string; onPress: () => void };
  /** action 대신 임의의 우측 요소를 넣고 싶을 때 */
  right?: ReactNode;
};

const SIDE_WIDTH = 56;

/**
 * 화면 상단 헤더 (Figma: 흰 배경, 가운데 타이틀, 좌측 뒤로가기, 우측 텍스트 액션).
 * 상태바 높이는 safe-area inset으로 자동 확보한다.
 */
export function Header({ title, onBack, action, right }: HeaderProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <View
      style={[
        styles.container,
        {
          paddingTop: insets.top,
          paddingHorizontal: theme.space.md,
          backgroundColor: theme.color.background.surface,
        },
      ]}
    >
      <View style={styles.row}>
        <View style={styles.side}>
          {onBack ? (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="뒤로가기"
              onPress={onBack}
              hitSlop={8}
            >
              <Ionicons name="chevron-back" size={24} color={theme.color.text.strong} />
            </Pressable>
          ) : null}
        </View>

        <Text
          numberOfLines={1}
          style={[theme.typography.body, styles.title, { color: theme.color.text.strong }]}
        >
          {title}
        </Text>

        <View style={[styles.side, styles.sideRight]}>
          {right ??
            (action ? (
              <Pressable accessibilityRole="button" onPress={action.onPress} hitSlop={8}>
                <Text style={[theme.typography.label, { color: theme.color.text.default }]}>
                  {action.label}
                </Text>
              </Pressable>
            ) : null)}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '100%',
  },
  row: {
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
  },
  side: {
    // width 고정이 아니라 minWidth — 짧은 콘텐츠(뒤로가기 화살표, "취소")는 예전과 똑같이
    // 56px로 보이고, "구매 내역" 같은 넓은 pill 버튼은 줄바꿈 없이 필요한 만큼 넓어진다.
    minWidth: SIDE_WIDTH,
    justifyContent: 'center',
  },
  sideRight: {
    alignItems: 'flex-end',
  },
  title: {
    flex: 1,
    fontSize: 17,
    textAlign: 'center',
  },
});
