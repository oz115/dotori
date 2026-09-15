import { Image, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

/** 상품 등록 화면 상단 배너. 목록 화면의 StoreBanner와 별개 — 문구·마스코트 포즈가 다르다. */
export function StoreProductFormBanner() {
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
          어떤 상품을 등록할까요?
        </Text>
        <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
          우리 아이를 위한 특별한 상품을 추가해보세요!
        </Text>
      </View>
      <Image
        source={require('@/assets/images/store/mascot-register.png')}
        style={styles.mascot}
        resizeMode="contain"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  banner: { flexDirection: 'row', alignItems: 'center' },
  mascot: { width: 104, height: 104 },
});
