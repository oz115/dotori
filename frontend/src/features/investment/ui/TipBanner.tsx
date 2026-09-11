import { Image, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

const ACORN = require('@/assets/images/acorn.png'); // 734 x 806

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
      <Image source={ACORN} style={styles.acorn} resizeMode="contain" />
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
  acorn: {
    width: 32,
    height: 35,
  },
  message: {
    flex: 1,
  },
});
