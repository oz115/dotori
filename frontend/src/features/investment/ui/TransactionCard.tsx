import { Image, StyleSheet, Text, View } from 'react-native';

import { changeColor } from '@/features/investment/lib/change';
import { formatAcorns, profitLabel } from '@/features/investment/lib/format';
import type { InvestmentTransactionItem } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  item: InvestmentTransactionItem;
};

/**
 * 거래 내역의 한 줄 카드 — 종목 아이콘·이름·거래 날짜와, 샀는지/팔았는지 배지,
 * 도토리가 나갔는지(-)/들어왔는지(+)를 보여준다.
 */
export function TransactionCard({ item }: Props) {
  const theme = useTheme();

  const bought = item.type === 'BUY';
  const amountColor = bought ? theme.color.text.strong : theme.color.investment.rise;
  const badgeBg = bought ? theme.color.brand.subtle : theme.color.status.info.subtle;
  const badgeText = bought ? theme.color.brand.subtleText : theme.color.investment.rise;

  return (
    <View
      style={[
        styles.card,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.xl,
          borderColor: theme.color.border.card,
          padding: theme.space.lg,
          gap: theme.space.md,
        },
      ]}
    >
      {/* 아이콘 원본이 흰 배경이라 배경색 없이 둥근 모서리로만 자른다 */}
      <Image
        source={item.image}
        style={[styles.icon, { borderRadius: theme.radius.md }]}
        resizeMode="cover"
      />

      <View style={[styles.nameBlock, { gap: 2 }]}>
        <Text
          numberOfLines={1}
          style={[theme.typography.label, { color: theme.color.text.strong }]}
        >
          {item.productName}
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
          {item.date}
        </Text>
        {!bought && item.buyAmount != null ? (
          <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
            구매 {formatAcorns(item.buyAmount)} → 판매 {formatAcorns(item.totalAmount)}
          </Text>
        ) : null}
        {!bought ? (
          <Text
            style={[
              theme.typography.bodySm,
              {
                color:
                  item.realizedProfit == null
                    ? theme.color.text.sub
                    : changeColor(item.realizedProfit, theme.color),
              },
            ]}
          >
            {item.realizedProfit == null
              ? '판매 수익을 계산할 정보가 부족해요.'
              : profitLabel(item.realizedProfit)}
          </Text>
        ) : null}
      </View>

      <View style={[styles.amountBlock, { gap: theme.space.xs }]}>
        <View style={[styles.badge, { backgroundColor: badgeBg, borderRadius: theme.radius.full }]}>
          <Text style={[theme.typography.caption, { color: badgeText }]}>
            {bought ? '산 금액' : '판 금액'}
          </Text>
        </View>
        <Text style={[theme.typography.label, { color: amountColor }]}>
          {bought ? '-' : '+'}
          {formatAcorns(item.totalAmount)}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  // 상점 상품 카드와 같은 문법 — 뚜렷한 1px 테두리 + 옅은 그림자로 카드 경계를 살린다
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
  },
  icon: {
    width: 44,
    height: 44,
  },
  nameBlock: {
    flex: 1,
  },
  amountBlock: {
    alignItems: 'flex-end',
  },
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
});
