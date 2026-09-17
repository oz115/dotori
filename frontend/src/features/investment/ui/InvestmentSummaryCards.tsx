import { Image, StyleSheet, Text, View, type ImageSourcePropType } from 'react-native';

import { formatPoints } from '@/features/investment/lib/format';
import type { InvestmentSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

// 여우 마스코트(설명하기 포즈) — 투자 화면의 안내 캐릭터 (보유 화면의 다람쥐와 짝)
const FOX = require('@/assets/images/invest/fox.png'); // 400 x 424
// 앱 공용 아이콘 시트에서 자른 지갑(넣어둔 돈)·새싹(자라서 번 돈)
const WALLET = require('@/assets/images/invest/wallet.png');
const SPROUT = require('@/assets/images/invest/sprout.png');

type Props = {
  summary: InvestmentSummary;
};

/**
 * 화면 상단의 투자 요약 카드 — 안내 문구·여우 캐릭터와 넣은 돈/번 돈 타일.
 * 할 일·상점 배너, 보유 화면 요약(HoldingsSummaryCard)과 같은 패밀리 룩
 * (연녹색 카드 + 문구 + 캐릭터 + 흰 타일)이다.
 */
export function InvestmentSummaryCards({ summary }: Props) {
  const theme = useTheme();

  const earnedColor =
    summary.earnedPoints >= 0 ? theme.color.status.success.text : theme.color.status.danger.text;

  const tiles: { label: string; icon: ImageSourcePropType; value: number; color?: string }[] = [
    { label: '넣은 돈', icon: WALLET, value: summary.investedPoints },
    { label: '번 돈', icon: SPROUT, value: summary.earnedPoints, color: earnedColor },
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
            오늘은 어디에 투자해볼까요?
          </Text>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            뉴스를 읽고 오를 회사를 찾아보세요!
          </Text>
        </View>
        <Image source={FOX} style={styles.mascot} resizeMode="contain" />
      </View>

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
                gap: theme.space.md,
              },
            ]}
          >
            <Image source={tile.icon} style={styles.tileIcon} resizeMode="contain" />
            <View style={[styles.tileTexts, { gap: theme.space.xs }]}>
              {/* 아이 화면이라 라벨도 또렷하게 — caption+muted 는 멀리서 안 읽힌다 */}
              <Text
                numberOfLines={1}
                style={[theme.typography.bodySm, { color: theme.color.text.sub }]}
              >
                {tile.label}
              </Text>
              <Text
                numberOfLines={1}
                adjustsFontSizeToFit
                style={[theme.typography.titleMd, { color: tile.color ?? theme.color.text.strong }]}
              >
                {formatPoints(tile.value)}
              </Text>
            </View>
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
    width: 84,
    height: 89,
  },
  tiles: {
    flexDirection: 'row',
  },
  tile: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: StyleSheet.hairlineWidth,
  },
  tileIcon: {
    width: 32,
    height: 32,
  },
  tileTexts: {
    flex: 1,
  },
});
