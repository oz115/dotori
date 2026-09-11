import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Polyline, Svg } from 'react-native-svg';

import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import { sparklinePoints } from '@/features/investment/lib/sparkline';
import type { StockSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  stock: StockSummary;
  onPress?: () => void;
};

const SPARK_WIDTH = 56;
const SPARK_HEIGHT = 26;

/**
 * 주식 리스트의 종목 한 줄 카드. 종목 아이콘·이름·업종, 미니 추이 차트,
 * 현재가와 전일 대비 등락 칩을 한 줄에 보여준다.
 */
export function StockCard({ stock, onPress }: Props) {
  const theme = useTheme();

  const rising = stock.change >= 0;
  const chipColors = rising ? theme.color.status.success : theme.color.status.danger;
  const points = sparklinePoints(stock.trend, SPARK_WIDTH, SPARK_HEIGHT);

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${stock.name} 상세 보기`}
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
      {/* 종목 아이콘 자리 표시자 — Figma 에셋 export 후 이미지로 교체 */}
      <View
        style={[
          styles.icon,
          { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.md },
        ]}
      >
        <Text style={styles.iconEmoji}>{stock.emoji}</Text>
      </View>

      <View style={styles.nameBlock}>
        <Text
          numberOfLines={1}
          style={[theme.typography.label, { color: theme.color.text.strong }]}
        >
          {stock.name}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          {stock.category}
        </Text>
      </View>

      {points ? (
        <Svg width={SPARK_WIDTH} height={SPARK_HEIGHT}>
          <Polyline
            points={points}
            fill="none"
            stroke={theme.color.brand.solid}
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </Svg>
      ) : null}

      <View style={[styles.priceBlock, { gap: theme.space.xs }]}>
        <View style={styles.priceRow}>
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>현재가</Text>
          <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
            {formatAcorns(stock.currentPrice)}
          </Text>
        </View>
        <View style={styles.priceRow}>
          <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
            전일 대비
          </Text>
          <View
            style={[
              styles.chip,
              { backgroundColor: chipColors.subtle, borderRadius: theme.radius.full },
            ]}
          >
            <Text style={[theme.typography.caption, { color: chipColors.text }]}>
              {signedAcorns(stock.change)}
            </Text>
          </View>
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
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconEmoji: {
    fontSize: 24,
  },
  nameBlock: {
    flex: 1,
    gap: 2,
  },
  priceBlock: {
    alignItems: 'flex-end',
  },
  priceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  chip: {
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
});
