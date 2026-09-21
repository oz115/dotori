import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { changeColor, changeLabel } from '@/features/investment/lib/change';
import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import type { Holding } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

const ACORN = require('@/assets/images/acorn.png'); // 734 x 806

type Props = {
  holding: Holding;
  onPress?: () => void;
  /** 꾹 누르기 — 여러 종목을 골라 한 번에 파는 선택 모드로 들어간다 */
  onLongPress?: () => void;
  /** 선택 모드면 오른쪽에 체크 표시가 나온다 (탭 동작은 부모가 onPress 로 바꿔 끼운다) */
  selectionMode?: boolean;
  selected?: boolean;
};

/**
 * 보유 화면의 종목 한 줄 카드 — 아이콘·이름과 지금 가격, 산 가격 대비 수익.
 * 이 화면의 종목은 전부 보유중이라 배지는 두지 않는다 (이름 가독성이 우선).
 * 카드를 꾹 누르면 선택 모드가 되어 여러 종목을 한 번에 팔 수 있다.
 */
export function HoldingCard({ holding, onPress, onLongPress, selectionMode, selected }: Props) {
  const theme = useTheme();

  const earned = (holding.currentPrice - holding.buyPrice) * holding.quantity;
  const color = changeColor(earned, theme.color);

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${holding.name} 상세 보기`}
      onPress={onPress}
      onLongPress={onLongPress}
      delayLongPress={300}
      style={({ pressed }) => [
        styles.card,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.xl,
          borderColor: selected ? theme.color.brand.solid : theme.color.border.card,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.85 : 1,
        },
      ]}
    >
      {/* 아이콘 원본이 흰 배경이라 배경색 없이 둥근 모서리로만 자른다 */}
      <Image
        source={holding.image}
        style={[styles.icon, { borderRadius: theme.radius.md }]}
        resizeMode="cover"
      />

      <Text
        numberOfLines={1}
        style={[theme.typography.label, styles.name, { color: theme.color.text.strong }]}
      >
        {holding.name}
      </Text>

      <View style={[styles.priceBlock, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          {formatAcorns(holding.currentPrice)}
        </Text>
        <View style={styles.change} accessibilityLabel={changeLabel(earned)}>
          <Image source={ACORN} style={styles.acorn} resizeMode="contain" />
          <Text style={[theme.typography.label, { color }]}>
            {earned === 0 ? '변동 없음' : signedAcorns(earned)}
          </Text>
        </View>
      </View>

      {selectionMode ? (
        <Ionicons
          name={selected ? 'checkmark-circle' : 'ellipse-outline'}
          size={24}
          color={selected ? theme.color.brand.solid : theme.color.text.muted}
        />
      ) : null}
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
    width: 52,
    height: 52,
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
