import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import { summarizeHoldings } from '@/features/investment/lib/holdings';
import type { Holding } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

// Figma 에셋 — 마스코트 시트의 다람쥐와 아이콘 시트의 도토리·동전·새싹
const SQUIRREL = require('@/assets/images/invest/squirrel.png');
const ACORN = require('@/assets/images/acorn.png'); // 734 x 806
const COIN = require('@/assets/images/invest/coin.png');
const SPROUT = require('@/assets/images/invest/sprout.png'); // "번 돈"은 투자 요약 카드와 같은 아이콘

type Props = {
  holdings: Holding[];
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

  const tiles: { key: string; icon: ImageSourcePropType; value: string; color?: string }[] = [
    { key: '가지고 있는 상품', icon: ACORN, value: `${summary.count}개` },
    { key: '지금 가치', icon: COIN, value: formatAcorns(summary.totalValue) },
    { key: '번 돈', icon: SPROUT, value: signedAcorns(summary.totalEarned), color: earnedColor },
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
      <View style={styles.headRow}>
        <View style={[styles.headTexts, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            지금까지 잘 투자하고 있어요! 🌱
          </Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            작은 투자가 모여 더 큰 경험이 될 거예요.
          </Text>
        </View>
        <Image source={SQUIRREL} style={styles.mascot} resizeMode="contain" />
      </View>

      <View style={[styles.tiles, { gap: theme.space.sm }]}>
        {tiles.map((tile) => (
          <View
            key={tile.key}
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
            <Image source={tile.icon} style={styles.tileIcon} resizeMode="contain" />
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
    width: 72,
    height: 72,
  },
  tiles: {
    flexDirection: 'row',
  },
  tile: {
    flex: 1,
    borderWidth: StyleSheet.hairlineWidth,
  },
  tileIcon: {
    width: 24,
    height: 24,
  },
});
