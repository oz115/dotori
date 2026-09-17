import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { formatPoints, signedPoints } from '@/features/investment/lib/format';
import { summarizeHoldings } from '@/features/investment/lib/holdings';
import type { Holding } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

// Figma 에셋 — 마스코트 시트의 다람쥐와 아이콘 시트의 도토리·동전·새싹
const SQUIRREL = require('@/assets/images/invest/squirrel.png');
const CANDLES = require('@/assets/images/invest/candles.png'); // 내 주식 = 주식 차트 아이콘
const COIN = require('@/assets/images/invest/coin.png');
const ACORN = require('@/assets/images/acorn.png'); // "번 돈"은 앱의 화폐인 도토리 (투자 요약 카드와 동일)

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
    // 타일이 3분할이라 좁다 — 라벨은 짧게, 값은 "10,000P"처럼 단위를 P로 붙인다
    // ("도토리" 단위는 좁은 타일에서 글자가 축소·클리핑돼 안 읽힌다. 메인 요약 타일과 같은 표기)
    { key: '내 주식', icon: CANDLES, value: `${summary.count}개` },
    { key: '지금 가치', icon: COIN, value: formatPoints(summary.totalValue) },
    { key: '번 돈', icon: ACORN, value: signedPoints(summary.totalEarned), color: earnedColor },
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
            <Text
              numberOfLines={1}
              style={[theme.typography.bodySm, { color: theme.color.text.sub }]}
            >
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
