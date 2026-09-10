import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type Props = {
  /** 좌측 아이콘 자리 표시자 — Figma 3D 에셋 export 후 이미지로 교체 */
  emoji: string;
  title: string;
  /** 우측 텍스트 액션 (예: "보유한 주식 보기") */
  action?: { label: string; onPress: () => void };
};

/**
 * 투자 화면의 섹션 제목 줄 (이모지 + 제목 + 우측 액션).
 */
export function SectionTitle({ emoji, title, action }: Props) {
  const theme = useTheme();

  return (
    <View style={styles.row}>
      <View style={[styles.left, { gap: theme.space.sm }]}>
        <Text style={styles.emoji}>{emoji}</Text>
        <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>{title}</Text>
      </View>
      {action ? (
        <Pressable
          accessibilityRole="button"
          onPress={action.onPress}
          hitSlop={8}
          style={styles.action}
        >
          <Text style={[theme.typography.label, { color: theme.color.text.default }]}>
            {action.label}
          </Text>
          <Ionicons name="chevron-forward" size={16} color={theme.color.text.default} />
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  left: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  emoji: {
    fontSize: 24,
  },
  action: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
});
