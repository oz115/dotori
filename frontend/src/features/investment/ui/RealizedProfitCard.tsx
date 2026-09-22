import { Image, StyleSheet, Text, View } from 'react-native';

import { changeColor } from '@/features/investment/lib/change';
import { profitLabel } from '@/features/investment/lib/format';
import { summarizeRealizedProfit } from '@/features/investment/lib/transactions';
import type { InvestmentTransactionItem } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

const ACORN = require('@/assets/images/acorn.png'); // 734 x 806

/** 매도 대금이 아닌, 매수 원금을 뺀 확정 손익만 합산한다. */
export function RealizedProfitCard({
  transactions,
}: {
  transactions: InvestmentTransactionItem[];
}) {
  const theme = useTheme();
  const { total, incomplete } = summarizeRealizedProfit(transactions);
  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.brand.subtle,
          borderRadius: theme.radius.xl,
          padding: theme.space.xl,
          gap: theme.space.md,
        },
      ]}
    >
      <View style={[styles.head, { gap: theme.space.md }]}>
        <Image source={ACORN} style={styles.acorn} resizeMode="contain" />
        <View style={[styles.texts, { gap: theme.space.xs }]}>
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>판매 수익</Text>
          <Text
            style={[
              theme.typography.titleMd,
              { color: incomplete ? theme.color.text.sub : changeColor(total, theme.color) },
            ]}
          >
            {incomplete ? '판매 수익을 계산할 정보가 부족해요.' : profitLabel(total)}
          </Text>
        </View>
      </View>
      {/* "판매금액 − 구매금액 = 판매 수익" 수식 안내는 뺐다 — 초등학생이 읽을 말이 아니고,
          제목("판매 수익")과 부호만으로 이미 무엇인지 읽힌다 (QA) */}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {},
  head: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  acorn: {
    width: 32,
    height: 35,
  },
  texts: {
    flex: 1,
  },
});
