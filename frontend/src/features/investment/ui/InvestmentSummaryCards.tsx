import { StyleSheet, Text, View } from 'react-native';

import { formatPoints } from '@/features/investment/lib/format';
import type { InvestmentSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  summary: InvestmentSummary;
};

/**
 * 화면 상단의 투자 요약 카드 2장 (넣은 돈 / 번 돈).
 * Figma 시안의 카드 그린이 semantic 토큰에 없어 palette.green 을 직접 쓴다
 * (semantic 에 어울리는 자리가 생기면 교체).
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
        <View
          key={card.label}
          style={[
            styles.card,
            {
              backgroundColor: theme.palette.green[300],
              borderRadius: theme.radius.lg,
              padding: theme.space.lg,
              gap: theme.space.xs,
            },
          ]}
        >
          <Text style={[theme.typography.caption, { color: theme.color.text.strong }]}>
            {card.label}
          </Text>
          <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>
            {formatPoints(card.value)}
          </Text>
        </View>
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
