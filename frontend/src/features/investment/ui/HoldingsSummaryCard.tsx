import { StyleSheet, Text, View } from 'react-native';

import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import { summarizeHoldings } from '@/features/investment/lib/holdings';
import type { StockDetail } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  holdings: StockDetail[];
};

/**
 * 보유 화면 상단 요약 카드 — 응원 문구와 합계 타일 3개
 * (가지고 있는 상품·지금 가치·번 돈).
 */
export function HoldingsSummaryCard({ holdings }: Props) {
  const theme = useTheme();
  const summary = summarizeHoldings(holdings);

  const earnedColor =
    summary.totalEarned >= 0 ? theme.color.status.success.text : theme.color.status.danger.text;

  const tiles: { key: string; emoji: string; value: string; color?: string }[] = [
    { key: '가지고 있는 상품', emoji: '🌰', value: `${summary.count}개` },
    { key: '지금 가치', emoji: '🪙', value: formatAcorns(summary.totalValue) },
    { key: '번 돈', emoji: '📊', value: signedAcorns(summary.totalEarned), color: earnedColor },
  ];

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.brand.tint,
          borderRadius: theme.radius.xl,
          padding: theme.space.xl,
          gap: theme.space.lg,
        },
      ]}
    >
      <View style={styles.headRow}>
        <View style={[styles.headTexts, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            지금까지 잘 투자하고 있어요! 🌱
          </Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            작은 투자가 모여 더 큰 경험이 될 거예요.
          </Text>
        </View>
        {/* 다람쥐 일러스트 자리 표시자 — Figma 에셋 export 후 이미지로 교체 */}
        <Text style={styles.mascot}>🐿️</Text>
      </View>

      <View style={[styles.tiles, { gap: theme.space.sm }]}>
        {tiles.map((tile) => (
          <View
            key={tile.key}
            style={[
              styles.tile,
              {
                backgroundColor: theme.color.background.surface,
                borderRadius: theme.radius.lg,
                padding: theme.space.md,
                gap: theme.space.xs,
              },
            ]}
          >
            <Text style={styles.tileEmoji}>{tile.emoji}</Text>
            <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
              {tile.key}
            </Text>
            <Text
              numberOfLines={1}
              adjustsFontSizeToFit
              style={[theme.typography.label, { color: tile.color ?? theme.color.text.strong }]}
            >
              {tile.value}
            </Text>
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {},
  headRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headTexts: {
    flex: 1,
    paddingRight: 8,
  },
  mascot: {
    fontSize: 44,
  },
  tiles: {
    flexDirection: 'row',
  },
  tile: {
    flex: 1,
  },
  tileEmoji: {
    fontSize: 20,
  },
});
