import { Image, StyleSheet, Text, View } from 'react-native';

import { formatPoints } from '@/features/investment/lib/format';
import type { InvestmentSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

// 여우 마스코트(설명하기 포즈) — 투자 화면의 안내 캐릭터 (보유 화면의 다람쥐와 짝)
const FOX = require('@/assets/images/invest/fox.png'); // 400 x 424

type Props = {
  summary: InvestmentSummary;
};

/**
 * 화면 상단의 투자 요약 카드 — 넣은 돈/번 돈 타일과 여우 캐릭터.
 * 보유 화면 요약(HoldingsSummaryCard)과 같은 패밀리 룩(연녹색 카드 + 흰 타일 + 캐릭터)이다.
 */
export function InvestmentSummaryCards({ summary }: Props) {
  const theme = useTheme();

  const tiles = [
    { label: '넣은 돈', value: summary.investedPoints },
    { label: '번 돈', value: summary.earnedPoints },
  ];

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.xl,
          padding: theme.space.xl,
          gap: theme.space.lg,
        },
      ]}
    >
      <View style={[styles.tiles, { gap: theme.space.sm }]}>
        {tiles.map((tile) => (
          <View
            key={tile.label}
            style={[
              styles.tile,
              {
                backgroundColor: theme.color.background.surface,
                borderColor: theme.color.border.default,
                borderRadius: theme.radius.lg,
                padding: theme.space.md,
                gap: theme.space.xs,
              },
            ]}
          >
            <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
              {tile.label}
            </Text>
            <Text
              numberOfLines={1}
              adjustsFontSizeToFit
              style={[theme.typography.titleMd, { color: theme.color.text.strong }]}
            >
              {formatPoints(tile.value)}
            </Text>
          </View>
        ))}
      </View>
      <Image source={FOX} style={styles.mascot} resizeMode="contain" />
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  tiles: {
    flex: 1,
  },
  tile: {
    borderWidth: StyleSheet.hairlineWidth,
  },
  mascot: {
    width: 96,
    height: 102,
  },
});
