import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { categoryBadgeColors } from '@/features/store/lib/categoryTheme';
import { categoryImage } from '@/features/store/lib/categoryImage';
import { categoryLabel } from '@/features/store/lib/categoryLabel';
import { isPriceReady, isSoldOut } from '@/features/store/lib/productStatus';
import type { StoreProductSummary } from '@/features/store/model/types';
import { useTheme } from '@/shared/theme';

type StoreProductCardProps = {
  product: StoreProductSummary;
  onPress?: () => void;
};

/** 상점 목록의 상품 카드 (SHOP-01). 탭하면 상세(SHOP-02)로 이동한다. */
export function StoreProductCard({ product, onPress }: StoreProductCardProps) {
  const theme = useTheme();
  const badge = categoryBadgeColors(theme, product.category);
  const soldOut = isSoldOut(product.quantity);
  const priceReady = isPriceReady(product.price);

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${product.name} 상세 보기`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.lg,
          opacity: pressed ? 0.9 : 1,
        },
      ]}
    >
      <View
        style={[
          styles.imageBox,
          {
            backgroundColor: badge.bg,
            borderTopLeftRadius: theme.radius.lg,
            borderTopRightRadius: theme.radius.lg,
            opacity: soldOut ? 0.5 : 1,
          },
        ]}
      >
        <Image source={categoryImage(product.category)} style={styles.image} resizeMode="contain" />
        {soldOut ? (
          <View style={[styles.soldOutBadge, { borderRadius: theme.radius.full }]}>
            <Text style={[theme.typography.caption, styles.soldOutText]}>품절</Text>
          </View>
        ) : null}
      </View>

      <View style={{ padding: theme.space.md, gap: theme.space.xs }}>
        <View
          style={[
            styles.categoryBadge,
            { backgroundColor: badge.bg, borderRadius: theme.radius.full, alignSelf: 'flex-start' },
          ]}
        >
          <Text style={[theme.typography.caption, { color: badge.text }]}>
            {categoryLabel(product.category)}
          </Text>
        </View>

        <Text
          numberOfLines={1}
          style={[theme.typography.label, { color: theme.color.text.strong }]}
        >
          {product.name}
        </Text>

        {product.description ? (
          <Text
            numberOfLines={1}
            style={[theme.typography.caption, { color: theme.color.text.muted }]}
          >
            {product.description}
          </Text>
        ) : null}

        <View style={[styles.footerRow, { marginTop: theme.space.xs }]}>
          {priceReady ? (
            <View style={styles.priceRow}>
              <Image
                source={require('@/assets/images/icons/icon-dotori.png')}
                style={styles.acornIcon}
                resizeMode="contain"
              />
              <Text style={[theme.typography.label, { color: theme.color.brand.subtleText }]}>
                {product.price?.toLocaleString('ko-KR')}
              </Text>
            </View>
          ) : (
            <View style={styles.priceRow}>
              <Ionicons name="sparkles" size={14} color={theme.color.brand.solid} />
              <Text
                numberOfLines={1}
                style={[
                  theme.typography.caption,
                  styles.priceText,
                  { color: theme.color.brand.subtleText },
                ]}
              >
                AI가 가격 산정 중
              </Text>
            </View>
          )}

          <Text
            numberOfLines={1}
            style={[
              styles.quantityText,
              theme.typography.caption,
              { color: theme.color.text.muted },
            ]}
          >
            재고 {product.quantity}개
          </Text>
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { flex: 1, overflow: 'hidden' },
  imageBox: {
    aspectRatio: 1.1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  image: { height: '88%', aspectRatio: 1 },
  soldOutBadge: {
    position: 'absolute',
    top: 8,
    right: 8,
    paddingHorizontal: 10,
    paddingVertical: 4,
    backgroundColor: 'rgba(39, 44, 38, 0.75)',
  },
  soldOutText: { color: '#FFFFFF' },
  categoryBadge: { paddingHorizontal: 10, paddingVertical: 3 },
  footerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 6,
  },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 4, flexShrink: 1, minWidth: 0 },
  priceText: { flexShrink: 1 },
  quantityText: { flexShrink: 0 },
  acornIcon: { width: 16, height: 16 },
});
