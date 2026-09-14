import { Image, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

/** 상점 화면 상단 배너. Figma 마스코트 일러스트가 없어 기존 도토리 아이콘으로 대체한다. */
export function StoreBanner() {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.banner,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.xl,
          padding: theme.space.lg,
          gap: theme.space.md,
        },
      ]}
    >
      <View style={{ flex: 1, gap: theme.space.xs }}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          우리 가족 상점
        </Text>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
          도토리로 원하는 보상을 골라보세요!
        </Text>
      </View>
      <Image
        source={require('@/assets/images/acorn.png')}
        style={styles.mascot}
        resizeMode="contain"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  banner: { flexDirection: 'row', alignItems: 'center' },
  mascot: { width: 56, height: 56 },
});
