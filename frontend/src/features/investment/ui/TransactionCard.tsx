import { Image, StyleSheet, Text, View } from 'react-native';

import { changeColor } from '@/features/investment/lib/change';
import { formatAcorns } from '@/features/investment/lib/format';
import { sellOutcomeMessage } from '@/features/investment/lib/orderMessages';
import type { InvestmentTransactionItem } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  item: InvestmentTransactionItem;
};

/**
 * 거래 내역의 한 줄 카드. 매수는 산 금액을, 매도는 "N 도토리를 벌었어요/잃었어요" 한 줄만
 * 보여준다 — 살 때/팔 때 가격까지 늘어놓으면 정보가 많아 못 읽는다는 피드백으로 줄였다
 * (매도 완료 알림과 같은 문구).
 */
export function TransactionCard({ item }: Props) {
  const theme = useTheme();

  const bought = item.type === 'BUY';
  const badgeBg = bought ? theme.color.brand.subtle : theme.color.status.info.subtle;
  const badgeText = bought ? theme.color.brand.subtleText : theme.color.investment.rise;
  const outcomeColor =
    item.realizedProfit == null
      ? theme.color.text.strong
      : changeColor(item.realizedProfit, theme.color);

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
      </View>

      <View style={[styles.amountBlock, { gap: theme.space.xs }]}>
        <View style={[styles.badge, { backgroundColor: badgeBg, borderRadius: theme.radius.full }]}>
          <Text style={[theme.typography.caption, { color: badgeText }]}>
            {bought ? '샀어요' : '팔았어요'}
          </Text>
        </View>
        {bought ? (
          <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
            -{formatAcorns(item.totalAmount)}
          </Text>
        ) : (
          <Text style={[theme.typography.label, { color: outcomeColor }]}>
            {item.realizedProfit == null
              ? `+${formatAcorns(item.totalAmount)}`
              : sellOutcomeMessage(item.realizedProfit)}
          </Text>
        )}
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
