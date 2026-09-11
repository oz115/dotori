import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import type { StockDetail } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  detail: StockDetail;
  onPress?: () => void;
};

/**
 * 보유 화면의 종목 한 줄 카드 — 아이콘·이름·보유중 배지와 지금 가격·등락 칩.
 */
export function HoldingCard({ detail, onPress }: Props) {
  const theme = useTheme();

  const rising = detail.changeFromYesterday >= 0;
  const chipColors = rising ? theme.color.status.success : theme.color.status.danger;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${detail.name} 상세 보기`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.lg,
          borderColor: theme.color.border.default,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.85 : 1,
        },
      ]}
    >
      <View
        style={[
          styles.icon,
          { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.md },
        ]}
      >
        <Image source={detail.image} style={styles.iconImage} resizeMode="contain" />
      </View>

      <View style={[styles.nameBlock, { gap: theme.space.sm }]}>
        <Text
          numberOfLines={1}
          style={[theme.typography.label, styles.name, { color: theme.color.text.strong }]}
        >
          {detail.name}
        </Text>
        <View
          style={[
            styles.badge,
            { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.full },
          ]}
        >
          <Text style={[theme.typography.caption, { color: theme.color.brand.subtleText }]}>
            보유중
          </Text>
        </View>
      </View>

      <View style={[styles.priceBlock, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>지금 가격</Text>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          {formatAcorns(detail.currentPrice)}
        </Text>
        <View
          style={[
            styles.chip,
            { backgroundColor: chipColors.subtle, borderRadius: theme.radius.full },
          ]}
        >
          <Text style={[theme.typography.caption, { color: chipColors.text }]}>
            {signedAcorns(detail.changeFromYesterday)}
          </Text>
        </View>
      </View>

      <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: StyleSheet.hairlineWidth,
  },
  icon: {
    width: 52,
    height: 52,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconImage: {
    width: 44,
    height: 44,
  },
  nameBlock: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  name: {
    flexShrink: 1,
  },
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  priceBlock: {
    alignItems: 'flex-end',
  },
  chip: {
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
});
