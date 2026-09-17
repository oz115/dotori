import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import type { StockSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

const ACORN = require('@/assets/images/acorn.png'); // 734 x 806

type Props = {
  stock: StockSummary;
  onPress?: () => void;
};

/**
 * 주식 리스트의 종목 한 줄 카드 — 아이콘·이름과 현재가·전일 대비 등락.
 * 업종은 상세 화면에서만 보여준다 (목록에선 이름 가독성이 우선이라는 팀 결정).
 */
export function StockCard({ stock, onPress }: Props) {
  const theme = useTheme();

  const rising = stock.change >= 0;
  const chipColors = rising ? theme.color.status.success : theme.color.status.danger;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${stock.name} 상세 보기`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.xl,
          borderColor: theme.color.border.card,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.85 : 1,
        },
      ]}
    >
      {/* 아이콘 원본이 흰 배경이라 배경색 없이 둥근 모서리로만 자른다 */}
      <Image
        source={stock.image}
        style={[styles.icon, { borderRadius: theme.radius.md }]}
        resizeMode="cover"
      />

      <Text
        numberOfLines={1}
        style={[theme.typography.label, styles.name, { color: theme.color.text.strong }]}
      >
        {stock.name}
      </Text>

      <View style={[styles.priceBlock, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          {formatAcorns(stock.currentPrice)}
        </Text>
        <View style={styles.change}>
          <Image source={ACORN} style={styles.acorn} resizeMode="contain" />
          <Text style={[theme.typography.label, { color: chipColors.text }]}>
            {signedAcorns(stock.change)}
          </Text>
        </View>
      </View>
    </Pressable>
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
  // 이름이 남는 공간을 전부 가져가고, 넘치면 말줄임 — 가격 블록에 밀리지 않게 한다
  name: {
    flex: 1,
  },
  priceBlock: {
    alignItems: 'flex-end',
  },
  change: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  acorn: {
    width: 14,
    height: 15,
  },
});
