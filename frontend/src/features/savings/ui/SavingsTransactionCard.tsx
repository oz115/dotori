import { Image, StyleSheet, Text, View } from 'react-native';

import type { SavingsTransactionItem } from '@/features/savings/model/types';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';

type Props = {
  item: SavingsTransactionItem;
};

/**
 * 예적금 거래 이력의 한 줄 카드 — 상품 아이콘·이름·날짜와 무엇을 했는지 배지,
 * 도토리가 나갔는지(-)/들어왔는지(+)를 보여준다. 투자 거래 카드와 같은 문법.
 */
export function SavingsTransactionCard({ item }: Props) {
  const theme = useTheme();

  // 지갑에서 나간 돈(넣었어요)은 빨강, 들어온 돈(돌려받았어요·이자·만기)은 파랑으로 구분한다.
  // 방향은 sign 이 정한다 — 투자 색(rise=파랑/fall=빨강)과 같은 규칙이라 화면끼리 어긋나지 않는다.
  const out = item.sign === '-';
  const tone = out ? theme.color.status.danger : theme.color.status.info;
  const amountColor = tone.text;
  const badgeBg = tone.subtle;
  const badgeText = tone.text;

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
      <Image source={item.image} style={styles.icon} resizeMode="contain" />

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
          <Text style={[theme.typography.caption, { color: badgeText }]}>{item.badge}</Text>
        </View>
        <Text style={[theme.typography.label, { color: amountColor }]}>
          {item.sign}
          {formatPoint(item.amount)}
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
    width: 40,
    height: 40,
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
