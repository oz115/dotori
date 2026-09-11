import { StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

type Props = {
  message: string;
};

/**
 * 투자 화면 하단의 한 줄 교육 팁 배너 (Figma: 도토리 + 연두 배경).
 */
export function TipBanner({ message }: Props) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.banner,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.xl,
          padding: theme.space.xl,
          gap: theme.space.md,
        },
      ]}
    >
      {/* 도토리 일러스트 자리 표시자 — Figma 에셋 export 후 이미지로 교체 */}
      <Text style={styles.emoji}>🌰</Text>
      <Text style={[theme.typography.bodySm, styles.message, { color: theme.color.text.default }]}>
        {message}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  emoji: {
    fontSize: 28,
  },
  message: {
    flex: 1,
  },
});
