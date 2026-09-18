import { Image, StyleSheet, Text, View } from 'react-native';

import { savingsProductImage } from '@/features/savings/lib/savingsAssets';
import { savingsGroupOf } from '@/features/savings/lib/savingsView';
import type { SavingsProductResponse } from '@/features/savings/model/types';
import { savingsGroupLabel } from '@/features/savings/ui/SavingsGroupSegment';
import { useTheme } from '@/shared/theme';

type Props = {
  product: SavingsProductResponse;
};

/**
 * 상품 상세 상단의 연녹색 배너 — 아이콘 · 분류 배지 · 상품명 · 설명 (Figma).
 *
 * 아이 상세(SavingsProductDetail)와 부모 상세(ParentSavingsProductDetail)가 같이 쓴다.
 * 아래 내용은 역할마다 다르지만 "이게 무슨 상품인가"를 말하는 이 부분은 같아야 한다.
 */
export function SavingsProductHero({ product }: Props) {
  const theme = useTheme();
  const groupLabel = savingsGroupLabel(savingsGroupOf(product.productType));

  return (
    <View
      style={[
        styles.hero,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.xl,
          padding: theme.space['2xl'],
          gap: theme.space.md,
        },
      ]}
    >
      <Image
        source={savingsProductImage(product.productType)}
        style={styles.heroIcon}
        resizeMode="contain"
      />
      <View
        style={[
          styles.heroBadge,
          { backgroundColor: theme.color.brand.solid, borderRadius: theme.radius.full },
        ]}
      >
        <Text style={[theme.typography.caption, { color: theme.color.text.onBrand }]}>
          {groupLabel}
        </Text>
      </View>
      <Text style={[theme.typography.h2, { color: theme.color.text.strong }]}>{product.name}</Text>
      {product.description ? (
        <Text
          style={[theme.typography.bodySm, styles.heroDescription, { color: theme.color.text.sub }]}
        >
          {product.description}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  hero: {
    alignItems: 'center',
  },
  heroIcon: {
    width: 96,
    height: 96,
  },
  heroBadge: {
    paddingHorizontal: 12,
    paddingVertical: 4,
  },
  heroDescription: {
    textAlign: 'center',
  },
});
