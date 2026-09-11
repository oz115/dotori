import { LinearGradient } from 'expo-linear-gradient';
import { StyleSheet, Text, View } from 'react-native';

import { formatPoints } from '@/features/investment/lib/format';
import type { InvestmentSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  summary: InvestmentSummary;
};

/**
 * 화면 상단의 투자 요약 카드 2장 (넣은 돈 / 번 돈).
 * 로그인 버튼과 같은 그린 그라디언트(palette.gradient.deep)로 칠한다 — Figma 시안 색감.
 */
export function InvestmentSummaryCards({ summary }: Props) {
  const theme = useTheme();

  const cards = [
    { label: '넣은 돈', value: summary.investedPoints },
    { label: '번 돈', value: summary.earnedPoints },
  ];

  return (
    <View style={[styles.row, { gap: theme.space.md }]}>
      {cards.map((card) => (
        <LinearGradient
          key={card.label}
          colors={theme.palette.gradient.deep}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={[
            styles.card,
            {
              borderRadius: theme.radius.xl,
              padding: theme.space.lg,
              gap: theme.space.xs,
            },
          ]}
        >
          <Text style={[theme.typography.caption, { color: theme.color.text.onBrand }]}>
            {card.label}
          </Text>
          <Text style={[theme.typography.h3, { color: theme.color.text.onBrand }]}>
            {formatPoints(card.value)}
          </Text>
        </LinearGradient>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
  },
  card: {
    flex: 1,
    minHeight: 92,
  },
});
